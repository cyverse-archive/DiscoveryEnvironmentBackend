package org.iplantc.workflow.model;

import static org.iplantc.workflow.util.ValidationUtils.validateFieldLength;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.marshaler.BaseTemplateMarshaller;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a type of validation rule that can be performed.
 *
 * @author Dennis Roberts
 */
public class RuleType extends WorkflowElement {

    /**
     * True if this rule type has been deprecated.
     */
    private boolean deprecated;

    /**
     * Specifies the relative order in which rule types are displayed.
     */
    private int displayOrder;

    /**
     * The list of value types that this rule type can validate.
     */
    private Set<ValueType> valueTypes = new HashSet<ValueType>();

    /**
     * A format specification for describing individual rules of this type.
     */
    private String ruleDescriptionFormat;

    /**
     * The rule sub-type.
     */
    private RuleSubtype subtype;

    /**
     * @param deprecated true if this rule type is deprecated.
     */
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    /**
     * @return true if this rule type is deprecated.
     */
    public boolean isDeprecated() {
        return deprecated;
    }

    /**
     * @param displayOrder the new display order.
     */
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * @return the display order.
     */
    public int getDisplayOrder() {
        return displayOrder;
    }

    /**
     * @param valueTypes the new set of value types.
     */
    public void setValueTypes(Set<ValueType> valueTypes) {
        this.valueTypes = new HashSet<ValueType>(valueTypes);
    }

    /**
     * @return the set of value types.
     */
    public Set<ValueType> getValueTypes() {
        return valueTypes;
    }

    /**
     * @param ruleDescriptionFormat a format specification for describing individual rules of this type.
     */
    public void setRuleDescriptionFormat(String ruleDescriptionFormat) {
        validateFieldLength(this.getClass(), "ruleDescriptionFormat", ruleDescriptionFormat, 255);
        this.ruleDescriptionFormat = ruleDescriptionFormat;
    }

    /**
     * @return a format specification for describing individual rules of this type.
     */
    public String getRuleDescriptionFormat() {
        return ruleDescriptionFormat;
    }

    /**
     * @param subtype the new rule sub-type.
     */
    public void setSubtype(RuleSubtype subtype) {
        this.subtype = subtype;
    }

    /**
     * @return the rule sub-type.
     */
    public RuleSubtype getSubtype() {
        return subtype;
    }

    /**
     * @return the rule sub-type name.
     */
    public String getSubtypeName() {
        return subtype == null ? "" : subtype.getName();
    }

    /**
     * Creates a new empty rule type.
     */
    public RuleType() {
        super();
    }

    /**
     * Creates a new rule type with the given ID, name, label and description.
     *
     * @param id the rule type identifier.
     * @param name the rule type name.
     * @param label the rule type label.
     * @param description the rule type description.
     */
    public RuleType(String id, String name, String label, String description) {
        super(id, name, label, description);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void accept(BaseTemplateMarshaller marshaller) throws WorkflowException {
        marshaller.visit(this);
        marshaller.leave(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = super.toJson();
            JSONArray array = new JSONArray();
            for (ValueType valueType : valueTypes) {
                array.put(valueType.getName());
            }
            json.put("value_types", array);
            json.put("subtype", getSubtypeName());
            json.put("rule_description_format", StringUtils.defaultString(ruleDescriptionFormat));
            return json;
        }
        catch (JSONException e) {
            throw new WorkflowException("unable to format the JSON object", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object otherObject) {
        return otherObject instanceof RuleType ? super.equals(otherObject) : false;
    }
}
