/**
 * This file was copied and modified from org.jasig.services.persondir.support.AttributeNamedPersonImpl.
 * 
 * Copyright (c) 2000-2009, Jasig, Inc.
 * See license distributed with this file and available online at
 * https://www.ja-sig.org/svn/jasig-parent/tags/rel-9/license-header.txt
 */
package org.iplantc.persondir.support;

import java.util.List;
import java.util.Map;
import org.jasig.services.persondir.IPersonAttributes;

/**
 * An implementation of IPersonAttributes that treats multiple records for a single person separately.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class AttributeNamedMultirecordPersonImpl extends BaseMultirecordPersonImpl {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_USER_NAME_ATTRIBUTE = "username";
    
    private final String userNameAttribute;

    public AttributeNamedMultirecordPersonImpl(Map<String, List<Object>> attributes) {
        super(attributes);
        this.userNameAttribute = DEFAULT_USER_NAME_ATTRIBUTE;
    }

    public AttributeNamedMultirecordPersonImpl(String userNameAttribute, Map<String, List<Object>> attributes) {
        super(attributes);
        this.userNameAttribute = userNameAttribute;
    }

    public AttributeNamedMultirecordPersonImpl(IPersonAttributes personAttributes) {
        this(personAttributes.getName(), personAttributes.getAttributes());
    }

    /* (non-Javadoc)
     * @see java.security.Principal#getName()
     */
    @Override
    public String getName() {
        final Object attributeValue = this.getAttributeValue(this.userNameAttribute);
        return attributeValue == null ? null : attributeValue.toString();
    }
}
