package org.iplantc.persondir.support;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.jasig.services.persondir.support.BasePersonImpl;

/**
 * An implementation of IPersonAttributes that treats multiple records for a single person separately.
 *
 * @author Dennis Roberts
 */
public abstract class BaseMultirecordPersonImpl extends BasePersonImpl {

    public BaseMultirecordPersonImpl(Map<String, List<Object>> attributes) {
        super(attributes);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(1574945487, 827742191)
            .append(this.getName())
            .append(this.getAttributes())
            .toHashCode();
    }

    /**
     * @see java.lang.Object#equals()
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BaseMultirecordPersonImpl)) {
            return false;
        }
        if (this == o) {
            return true;
        }
        final BaseMultirecordPersonImpl other = (BaseMultirecordPersonImpl) o;
        return new EqualsBuilder()
            .append(this.getName(), other.getName())
            .append(this.getAttributes(), other.getAttributes())
            .isEquals();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("name", this.getName())
            .append("attributes", this.getAttributes())
            .toString();
    }
}
