package org.iplantc.persondir.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jasig.services.persondir.IPersonAttributeDao;
import org.jasig.services.persondir.IPersonAttributes;
import org.jasig.services.persondir.support.AbstractDefaultAttributePersonAttributeDao;
import org.jasig.services.persondir.support.NamedPersonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;

/**
 * An implementation of {@link IPersonAttributeDao} that accumulates multiple results into a single instance of
 * a class that implements {@link IPersonAttributes}.  This class is designed to wrap other implementations of
 * IPersonAttributeDao and intercept calls to getPerson().  The primary purpose of this class is to obtain multi-valued
 * person attributes from directory objects that are associated with a person.  For example, this class can be used
 * in conjunction with {@link LdapMultirecordAttributeDao} to obtain group memberships from an LDAP directory that does
 * not have the group memberships listed in the Person object.  The spring configuration would look something like this:
 *
 * <pre>
 * {@code 
 * <bean id="groupAttributeRepository" class="org.iplantc.persondir.support.AccumulatingPersonAttributeDao">
 *     <property name="innerDao">
 *         <bean class="org.iplantc.persondir.support.ldap.LdapMultirecordAttributeDao">
 *             <property name="contextSource" ref="contextSource" />
 *             <property name="requireAllQueryAttributes" value="true" />
 *             <property name="baseDN" value="ou=Groups,dc=iplantcollaborative,dc=org" />
 *              <property name="queryAttributeMapping">
 *                 <map>
 *                     <entry key="username" value="memberUid" />
 *                 </map>
 *              </property>
 *              <property name="resultAttributeMapping">
 *                 <map>
 *                     <entry key="cn" value="entitlement" />
 *                 </map>
 *             </property>
 *         </bean>
 *     </property>
 * </bean>
 * }
 * </pre>
 * 
 * @author Dennis Roberts
 */
public class AccumulatingPersonAttributeDao extends AbstractDefaultAttributePersonAttributeDao {

    /**
     * Used to log debugging messages.
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccumulatingPersonAttributeDao.class);

    /**
     * The DAO that actually retrieves the IPersonAttribute instances.
     */
    private IPersonAttributeDao innerDao;

    /**
     * @param innerDao the DAO that actually retrieves the IPersonAttribute instances.
     */
    public void setInnerDao(IPersonAttributeDao innerDao) {
        this.innerDao = innerDao;
    }

    /**
     * Builds and returns a single IPersonAttribute instance containing the accumulated attributes of all matching
     * query results.
     * 
     * @param uid the user ID.
     * @return the cumulative IPersonAttribute instance.
     */
    @Override
    public IPersonAttributes getPerson(String uid) {
        LOG.trace("getPerson called for uid: {}", uid);
        Validate.notNull(uid, "uid may not be null.");
        final Map<String, List<Object>> seed = toSeedMap(uid);
        final Set<IPersonAttributes> people = getPeopleWithMultivaluedAttributes(seed);
        return ensureNameValued(uid, (IPersonAttributes) DataAccessUtils.singleResult(people));
    }

    /**
     * Ensures that the user's name is valued in an instance of a class that implements IPersonAttributes.  If the
     * name is not valued then the user ID is used as the name.
     * 
     * @param uid the user ID.
     * @param person the IPersonAttributes instance.
     * @return an IPersonAttributes instance with the name valued.
     */
    private IPersonAttributes ensureNameValued(String uid, IPersonAttributes person) {
        return StringUtils.isEmpty(person.getName()) ? new NamedPersonImpl(uid, person.getAttributes()) : person;
    }

    /**
     * Builds and returns a set containing a single IPersonAttribute instance containing the accumulated attributes
     * of all matching query results.
     * 
     * @param query the query.
     * @return a set containing the single cumulative IPersonAttribute instance.
     */
    @Override
    public Set<IPersonAttributes> getPeopleWithMultivaluedAttributes(Map<String, List<Object>> query) {
        LOG.trace("getPeopleWithMultivaluedAttributes called for query: {}", query);
        Set<IPersonAttributes> people = innerDao.getPeopleWithMultivaluedAttributes(query);
        LOG.debug("innerDao.getPeopleWithMultivaluedAttributes returned {}", people);
        if (people.isEmpty()) {
            return people;
        }
        else {
            Set<IPersonAttributes> result = new HashSet<IPersonAttributes>();
            result.add(accumulateAttributes(people));
            return result;
        }
    }

    /**
     * Accumulates the attributes of a set of IPersonAttribute instances into a single IPersonAttribute instance.
     * 
     * @param people the set of IPersonAttribute instances.
     * @return the cumulative IPersonAttribute instance.
     */
    private IPersonAttributes accumulateAttributes(Set<IPersonAttributes> people) {
        String name = people.isEmpty() ? null : people.iterator().next().getName();
        Map<String, List<Object>> cumulativeAttributes = new HashMap<String, List<Object>>();
        for (IPersonAttributes person : people) {
            appendAttributes(cumulativeAttributes, person.getAttributes());
        }
        return new NamedPersonImpl(name, cumulativeAttributes);
    }

    /**
     * Appends attribute values from a source attribute map onto the end of equivalently named attributes in a
     * destination attribute map.
     * 
     * @param dest the destination attribute map.
     * @param source the source attribute map.
     */
    private void appendAttributes(Map<String, List<Object>> dest, Map<String, List<Object>> source) {
        for (Map.Entry<String, List<Object>> entry : source.entrySet()) {
            List<Object> sourceValue = entry.getValue();
            if (sourceValue != null) {
                String key = entry.getKey();
                List<Object> destValue = dest.get(key);
                if (destValue == null) {
                    destValue = new ArrayList<Object>(entry.getValue());
                    dest.put(key, destValue);
                    LOG.debug("new attribute: {} => {}", key, destValue);
                }
                else {
                    destValue.addAll(entry.getValue());
                    LOG.debug("updated attribute: {} => {}", key, destValue);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getPossibleUserAttributeNames() {
        return innerDao.getPossibleUserAttributeNames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAvailableQueryAttributes() {
        return innerDao.getAvailableQueryAttributes();
    }
}
