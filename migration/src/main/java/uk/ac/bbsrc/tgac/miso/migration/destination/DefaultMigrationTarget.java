package uk.ac.bbsrc.tgac.miso.migration.destination;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.eaglegenomics.simlims.core.SecurityProfile;
import com.eaglegenomics.simlims.core.User;

import uk.ac.bbsrc.tgac.miso.core.data.ChangeLog;
import uk.ac.bbsrc.tgac.miso.core.data.Library;
import uk.ac.bbsrc.tgac.miso.core.data.Pool;
import uk.ac.bbsrc.tgac.miso.core.data.Project;
import uk.ac.bbsrc.tgac.miso.core.data.Run;
import uk.ac.bbsrc.tgac.miso.core.data.Sample;
import uk.ac.bbsrc.tgac.miso.core.data.SampleAdditionalInfo;
import uk.ac.bbsrc.tgac.miso.core.data.SequencerPartitionContainer;
import uk.ac.bbsrc.tgac.miso.core.data.SequencerPoolPartition;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryDilution;
import uk.ac.bbsrc.tgac.miso.core.util.LimsUtils;
import uk.ac.bbsrc.tgac.miso.migration.MigrationData;
import uk.ac.bbsrc.tgac.miso.migration.MigrationProperties;

public class DefaultMigrationTarget implements MigrationTarget {
  
  private static final Logger log = Logger.getLogger(DefaultMigrationTarget.class);

  private static final String OPT_DB_HOST = "target.db.host";
  private static final String OPT_DB_PORT = "target.db.port";
  private static final String OPT_DB_NAME = "target.db.name";
  private static final String OPT_DB_USER = "target.db.user";
  private static final String OPT_DB_PASS = "target.db.pass";
  
  private static final String OPT_MISO_USER = "target.miso.user";
  
  private static final String OPT_DRY_RUN = "target.dryrun";
  private static final String OPT_REPLACE_CHANGELOGS = "target.replaceChangeLogs";
  
  private final SessionFactory sessionFactory;
  private final MisoServiceManager serviceManager;
  private final ValueTypeLookup valueTypeLookup;
  
  private boolean dryrun = false;
  private boolean replaceChangeLogs = false;
  
  private Date timeStamp;
  
  public DefaultMigrationTarget(MigrationProperties properties) throws IOException {
    this.timeStamp = new Date();
    this.dryrun = properties.getBoolean(OPT_DRY_RUN, false);
    this.replaceChangeLogs = properties.getBoolean(OPT_REPLACE_CHANGELOGS, false);
    DataSource datasource = makeDataSource(properties);
    DataSource dsProxy = new TransactionAwareDataSourceProxy(datasource);
    this.sessionFactory = MisoTargetUtils.makeSessionFactory(dsProxy);
    JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
    this.serviceManager = MisoServiceManager.buildWithDefaults(jdbcTemplate, sessionFactory,
        properties.getRequiredString(OPT_MISO_USER));
    this.valueTypeLookup = readInTransaction(new TransactionWork<ValueTypeLookup>() {
      @Override
      public ValueTypeLookup doWork() throws IOException {
        return new ValueTypeLookup(serviceManager);
      }
    });
    HibernateTransactionManager txManager = new HibernateTransactionManager(sessionFactory);
    txManager.setDataSource(datasource);
    txManager.setHibernateManagedSession(true);
    TransactionSynchronizationManager.initSynchronization();
  }
  
  private static DataSource makeDataSource(MigrationProperties properties) {
    String dbHost = properties.getRequiredString(OPT_DB_HOST);
    String dbPort = properties.getRequiredString(OPT_DB_PORT);
    String dbName = properties.getRequiredString(OPT_DB_NAME);
    String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?autoReconnect=true&zeroDateTimeBehavior=convertToNull"
        + "&useUnicode=true&characterEncoding=UTF-8";
    String username = properties.getRequiredString(OPT_DB_USER);
    String password = properties.getRequiredString(OPT_DB_PASS);
    return MisoTargetUtils.makeDataSource(url, username, password);
  }

  @Override
  public void migrate(final MigrationData data) throws IOException {
    log.info(dryrun ? "Doing a dry run" : "Changes will be saved");
    
    Transaction tx = sessionFactory.getCurrentSession().beginTransaction();
    try {
      doMigration(data);
      if (dryrun) {
        tx.rollback();
        log.info("Dry run successful and rolled back.");
      } else {
        tx.commit();
      }
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
  
  private void doMigration(MigrationData data) throws IOException {
    saveProjects(data.getProjects());
    saveSamples(data.getSamples());
    saveLibraries(data.getLibraries());
    saveLibraryDilutions(data.getDilutions());
    savePools(data.getPools());
    saveRuns(data.getRuns());
  }

  public void saveProjects(Collection<Project> projects) throws IOException {
    log.info("Migrating projects...");
    User user = serviceManager.getAuthorizationManager().getCurrentUser();
    for (Project project : projects) {
      project.setSecurityProfile(new SecurityProfile(user));
      serviceManager.getProjectDao().save(project);
      log.debug("Saved project " + project.getAlias());
    }
    log.info(projects.size() + " projects migrated.");
  }

  public void saveSamples(final Collection<Sample> samples) throws IOException {
    log.info("Migrating samples...");
    for (Sample sample : samples) {
      saveSample(sample);
    }
    log.info(samples.size() + " samples migrated.");
  }
  
  private void saveSample(Sample sample) throws IOException {
    if (sample.getId() != Sample.UNSAVED_ID) {
      // already saved
      return;
    }
    if (hasParent(sample)) {
      // save parent first to generate ID
      saveSample(((SampleAdditionalInfo) sample).getParent());
    }
    sample.inheritPermissions(sample.getProject());
    valueTypeLookup.resolveAll(sample);
    if (replaceChangeLogs) {
      Collection<ChangeLog> changes = new ArrayList<>(sample.getChangeLog());
      sample.setId(serviceManager.getSampleService().create(sample));
      sessionFactory.getCurrentSession().flush();
      saveSampleChangeLog(sample, changes);
    } else {
      sample.setId(serviceManager.getSampleService().create(sample));
    }
    log.debug("Saved sample " + sample.getAlias());
  }
  
  private static boolean hasParent(Sample sample) {
    return LimsUtils.isDetailedSample(sample) && ((SampleAdditionalInfo) sample).getParent() != null;
  }
  
  private void saveSampleChangeLog(Sample sample, Collection<ChangeLog> changes) throws IOException {
    if (changes == null || changes.isEmpty()) throw new IOException("Cannot save sample due to missing changelogs");
    serviceManager.getChangeLogDao().deleteAllById("sample", sample.getId());
    for (ChangeLog change : changes) {
      change.setUserId(serviceManager.getAuthorizationManager().getCurrentUser().getUserId());
      serviceManager.getChangeLogDao().create("sample", sample.getId(), change);
    }
  }

  public void saveLibraries(final Collection<Library> libraries) throws IOException {
    log.info("Migrating libraries...");
    User user = serviceManager.getAuthorizationManager().getCurrentUser();
    for (Library library : libraries) {
      library.inheritPermissions(library.getSample());
      valueTypeLookup.resolveAll(library);
      library.setLastModifier(user);
      library.setLastUpdated(timeStamp);
      library.getLibraryAdditionalInfo().setCreatedBy(user);
      library.getLibraryAdditionalInfo().setCreationDate(timeStamp);
      library.getLibraryAdditionalInfo().setUpdatedBy(user);
      library.getLibraryAdditionalInfo().setLastUpdated(timeStamp);
      if (replaceChangeLogs) {
        Collection<ChangeLog> changes = library.getChangeLog();
        library.setId(serviceManager.getLibraryDao().save(library));
        saveLibraryChangeLog(library, changes);
      } else {
        library.setId(serviceManager.getLibraryDao().save(library));
      }
      log.debug("Saved library " + library.getAlias());
    }
    log.info(libraries.size() + " libraries migrated.");
  }
  
  private void saveLibraryChangeLog(Library library, Collection<ChangeLog> changes) throws IOException {
    if (changes == null || changes.isEmpty()) throw new IOException("Cannot save library due to missing changelogs");
    serviceManager.getChangeLogDao().deleteAllById("library", library.getId());
    for (ChangeLog change : changes) {
      change.setUserId(serviceManager.getAuthorizationManager().getCurrentUser().getUserId());
      serviceManager.getChangeLogDao().create("library", library.getId(), change);
    }
  }

  public void saveLibraryDilutions(final Collection<LibraryDilution> libraryDilutions) throws IOException {
    log.info("Migrating library dilutions...");
    for (LibraryDilution ldi : libraryDilutions) {
      if (replaceChangeLogs) {
        if (ldi.getCreationDate() == null || ldi.getLastModified() == null) {
          throw new IOException("Cannot save dilution due to missing timestamps");
        }
      } else {
        ldi.setCreationDate(timeStamp);
        ldi.setLastModified(timeStamp);
      }
      
      ldi.setId(serviceManager.getDilutionDao().save(ldi));
      log.debug("Saved library dilution " + ldi.getName());
    }
    log.info(libraryDilutions.size() + " library dilutions migrated.");
  }

  public void savePools(final Collection<Pool<LibraryDilution>> pools) throws IOException {
    log.info("Migrating pools...");
    User user = serviceManager.getAuthorizationManager().getCurrentUser();
    for (Pool<LibraryDilution> pool : pools) {
      pool.setCreationDate(timeStamp);
      pool.setLastModifier(user);
      pool.setLastUpdated(timeStamp);
      pool.setId(serviceManager.getPoolDao().save(pool));
      log.debug("Saved pool " + pool.getAlias());
    }
    log.info(pools.size() + " pools migrated.");
  }

  public void saveRuns(final Collection<Run> runs) throws IOException {
    log.info("Migrating runs...");
    User user = serviceManager.getAuthorizationManager().getCurrentUser();
    for (Run run : runs) {
      for (SequencerPartitionContainer<SequencerPoolPartition> container : run.getSequencerPartitionContainers()) {
        container.setLastModifier(user);
      }
      run.setLastModifier(user);
      run.setId(serviceManager.getRunDao().save(run));
      log.debug("Saved run " + run.getAlias());
    }
    log.info(runs.size() + " runs migrated.");
  }
  
  /**
   * Performs work in a transaction, rolling back after it completes
   * 
   * @param work
   * @return
   * @throws IOException if any exception is thrown while in the transaction
   */
  private <T> T readInTransaction(TransactionWork<T> work) throws IOException {
    Transaction tx = sessionFactory.getCurrentSession().beginTransaction();
    try {
      T result = work.doWork();
      return result;
    } finally {
      tx.rollback();
    }
  }
  
  /**
   * Functional interface for work to be done in a transaction
   * 
   * @param <T> return type of work
   */
  private static interface TransactionWork<T> {
    public T doWork() throws IOException;
  }

}
