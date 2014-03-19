package org.iplantc.persistence.dao.hibernate.data;

import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.data.IntegrationDatumDao;
import org.iplantc.persistence.dao.hibernate.AbstractHibernateDao;
import org.iplantc.persistence.dto.data.IntegrationDatum;

/**
 * @author Dennis Roberts
 */
public class HibernateIntegrationDatumDao extends AbstractHibernateDao<IntegrationDatum>
        implements IntegrationDatumDao {

    /**
     * @param session the Hibernate session.
     */
    public HibernateIntegrationDatumDao(Session session) {
        super(IntegrationDatum.class, session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrationDatum findByNameAndEmail(String name, String email) {
        Query query = getNamedQuery("findByNameAndEmail");
        query.setParameter("name", name);
        query.setParameter("email", email);
        return (IntegrationDatum) query.uniqueResult();
    }
}
