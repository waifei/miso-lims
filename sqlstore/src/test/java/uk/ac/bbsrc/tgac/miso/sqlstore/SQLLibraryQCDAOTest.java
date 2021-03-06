/**
 * 
 */
package uk.ac.bbsrc.tgac.miso.sqlstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;

import javax.persistence.CascadeType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import uk.ac.bbsrc.tgac.miso.AbstractDAOTest;
import uk.ac.bbsrc.tgac.miso.core.data.Library;
import uk.ac.bbsrc.tgac.miso.core.data.LibraryQC;
import uk.ac.bbsrc.tgac.miso.core.data.impl.LibraryQCImpl;
import uk.ac.bbsrc.tgac.miso.core.data.type.QcType;
import uk.ac.bbsrc.tgac.miso.core.exception.MalformedLibraryException;
import uk.ac.bbsrc.tgac.miso.core.factory.TgacDataObjectFactory;
import uk.ac.bbsrc.tgac.miso.core.store.LibraryStore;

/**
 * @author Chris Salt
 *
 */
public class SQLLibraryQCDAOTest extends AbstractDAOTest {

  @Autowired
  @Spy
  private JdbcTemplate jdbcTemplate;

  @Mock
  private LibraryStore libraryDAO;

  @InjectMocks
  private SQLLibraryQCDAO dao;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    dao.setDataObjectFactory(new TgacDataObjectFactory());

  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#save(uk.ac.bbsrc.tgac.miso.core.data.LibraryQC)}.
   * 
   * @throws IOException
   * @throws MalformedLibraryException
   */
  @Test
  public void testSave() throws IOException, MalformedLibraryException {
    LibraryQC qc = new LibraryQCImpl();
    qc.setLibrary(Mockito.mock(Library.class));
    qc.setQcType(Mockito.mock(QcType.class));
    long id = dao.save(qc);

    LibraryQC saved = dao.get(id);
    assertEquals(qc, saved);
  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#get(long)}.
   * 
   * @throws IOException
   */
  @Test
  public void testGet() throws IOException {
    LibraryQC qc = dao.get(1L);
    assertNotNull(qc);
    assertEquals("admin", qc.getQcCreator());
  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#lazyGet(long)}.
   * 
   * @throws IOException
   */
  @Test
  public void testLazyGet() throws IOException {
    LibraryQC qc = dao.lazyGet(1L);
    assertNotNull(qc);
    assertEquals("admin", qc.getQcCreator());
  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#listByLibraryId(long)}.
   * 
   * @throws IOException
   */
  @Test
  public void testListByLibraryId() throws IOException {
    List<LibraryQC> libraryQcs = (List<LibraryQC>) dao.listByLibraryId(1L);
    assertNotNull(libraryQcs);
    assertEquals(1, libraryQcs.size());
  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#listAll()}.
   * 
   * @throws IOException
   */
  @Test
  public void testListAll() throws IOException {
    List<LibraryQC> libraryQcs = (List<LibraryQC>) dao.listAll();
    assertNotNull(libraryQcs);
    assertEquals(14, libraryQcs.size());
  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#count()}.
   * 
   * @throws IOException
   */
  @Test
  public void testCount() throws IOException {
    int count = dao.count();
    assertEquals(14, count);
  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#remove(uk.ac.bbsrc.tgac.miso.core.data.LibraryQC)}.
   * 
   * @throws IOException
   */
  @Test
  public void testRemove() throws IOException {
    LibraryQC libraryQc = dao.get(1L);
    assertNotNull(libraryQc);
    dao.setCascadeType(CascadeType.REMOVE);
    dao.remove(libraryQc);
    LibraryQC qc = dao.get(1L);
    assertNull(qc);
  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#listAllLibraryQcTypes()}.
   * 
   * @throws IOException
   */
  @Test
  public void testListAllLibraryQcTypes() throws IOException {
    List<QcType> types = (List<QcType>) dao.listAllLibraryQcTypes();
    assertNotNull(types);
    assertEquals(3, types.size());

  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#getLibraryQcTypeById(long)}.
   * 
   * @throws IOException
   */
  @Test
  public void testGetLibraryQcTypeById() throws IOException {
    QcType type = dao.getLibraryQcTypeById(1L);
    assertEquals("qPCR", type.getName());
  }

  /**
   * Test method for {@link uk.ac.bbsrc.tgac.miso.sqlstore.SQLLibraryQCDAO#getLibraryQcTypeByName(java.lang.String)}.
   * 
   * @throws IOException
   */
  @Test
  public void testGetLibraryQcTypeByName() throws IOException {
    QcType type = dao.getLibraryQcTypeByName("qPCR");
    assertEquals(new Long(1), type.getQcTypeId());
  }

}
