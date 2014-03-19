/**
 * This class was copied and modified from org.jasig.services.persondir.support.ldap.LogicalFilterMapper.
 * 
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-9/license-header.txt
 */
package org.iplantc.persondir.support.ldap;

import org.jasig.services.persondir.support.QueryType;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.Filter;
import org.springframework.ldap.filter.OrFilter;

/**
 * Wrapper class to allow the And and Or filters to be treated the same way.
 * 
 * @author Eric Dalquist
 * @version $Revision: 1.1 $
 */
class LogicalFilterWrapper implements Filter {

    private final QueryType queryType;

    private final AndFilter andFilter;

    private final OrFilter orFilter;

    private final Filter delegateFilter;

    public LogicalFilterWrapper(QueryType queryType) {
        this.queryType = queryType;
        if (queryType == QueryType.OR) {
            this.andFilter = null;
            this.orFilter = new OrFilter();

            this.delegateFilter = this.orFilter;
        }
        else {
            this.andFilter = new AndFilter();
            this.orFilter = null;

            this.delegateFilter = this.andFilter;
        }
    }

    /**
     * Append the query Filter to the underlying logical Filter
     */
    public void append(Filter query) {
        if (queryType == QueryType.OR) {
            this.orFilter.or(query);
        }
        else {
            this.andFilter.and(query);
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.ldap.filter.Filter#encode()
     */
    @Override
    public String encode() {
        return this.delegateFilter.encode();
    }

    /* (non-Javadoc)
     * @see org.springframework.ldap.filter.Filter#encode(java.lang.StringBuffer)
     */
    @Override
    public StringBuffer encode(StringBuffer buf) {
        return this.delegateFilter.encode(buf);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return this.delegateFilter.equals(o);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.delegateFilter.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.delegateFilter.toString();
    }
}