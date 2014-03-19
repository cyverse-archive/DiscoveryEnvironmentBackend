package org.iplantc.persistence.dao.hibernate.refgenomes;

import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.iplantc.persistence.dao.hibernate.AbstractHibernateDao;
import org.iplantc.persistence.dao.refgenomes.ReferenceGenomeDao;
import org.iplantc.persistence.dto.refgenomes.ReferenceGenome;

/**
 * @author Dennis Roberts
 */
public class HibernateReferenceGenomeDao extends AbstractHibernateDao<ReferenceGenome> implements ReferenceGenomeDao {

    /**
     * @param session the Hibernate session.
     */
    public HibernateReferenceGenomeDao(Session session) {
        super(ReferenceGenome.class, session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceGenome findByUuid(String id) {
        Query query = getNamedQuery("findByUuid");
        query.setString("id", id);
        return (ReferenceGenome) query.uniqueResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ReferenceGenome> list() {
        Query query = getNamedQuery("list");
        return query.list();
    }
}
