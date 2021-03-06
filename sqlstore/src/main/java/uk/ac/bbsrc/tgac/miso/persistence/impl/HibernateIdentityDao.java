package uk.ac.bbsrc.tgac.miso.persistence.impl;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.bbsrc.tgac.miso.core.data.Identity;
import uk.ac.bbsrc.tgac.miso.core.data.impl.IdentityImpl;
import uk.ac.bbsrc.tgac.miso.persistence.IdentityDao;

@Repository
@Transactional(rollbackFor = Exception.class)
public class HibernateIdentityDao implements IdentityDao {

  protected static final Logger log = LoggerFactory.getLogger(HibernateIdentityDao.class);

  @Autowired
  private SessionFactory sessionFactory;

  private Session currentSession() {
    return getSessionFactory().getCurrentSession();
  }

  @Override
  public List<Identity> getIdentity() {
    Query query = currentSession().createQuery("from IdentityImpl");
    @SuppressWarnings("unchecked")
    List<Identity> records = query.list();
    return records;
  }

  @Override
  public Identity getIdentity(Long id) {
    return (Identity) currentSession().get(IdentityImpl.class, id);
  }

  @Override
  public Identity getIdentity(String externalName) {
    Query query = currentSession().createQuery("FROM IdentityImpl I WHERE I.externalName = :externalName");
    query.setParameter("externalName", externalName);
    return (Identity) query.uniqueResult();
  }

  @Override
  public void deleteIdentity(Identity identity) {
    currentSession().delete(identity);

  }

  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

}
