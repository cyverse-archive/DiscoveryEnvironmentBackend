/**
 * This file was copied and modified from org.jasig.services.persondir.support.NamedPersonImpl.
 * 
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-9/license-header.txt
 */
package org.iplantc.persondir.support;

import java.util.List;
import java.util.Map;

/**
 * An implementation of IPersonAttributes that treats multiple records for a single person separately.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class NamedMultirecordPersonImpl extends BaseMultirecordPersonImpl {
    private static final long serialVersionUID = 1L;

    private final String userName;

    public NamedMultirecordPersonImpl(String userName, Map<String, List<Object>> attributes) {
        super(attributes);
        this.userName = userName;
    }

    /* (non-Javadoc)
     * @see java.security.Principal#getName()
     */
    @Override
    public String getName() {
        return this.userName;
    }
}
