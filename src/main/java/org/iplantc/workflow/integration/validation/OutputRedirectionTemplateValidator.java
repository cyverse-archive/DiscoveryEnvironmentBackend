package org.iplantc.workflow.integration.validation;

import org.apache.commons.lang.StringUtils;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;

/**
 * Verifies that a template has at most one output property that is mapped to standard output.
 *
 * @author Dennis Roberts
 */
public class OutputRedirectionTemplateValidator implements TemplateValidator {

    /**
     * The name of the output stream to validate.
     */
    private String streamName;

    /**
     * @param streamName the name of the output stream to validate.
     */
    public OutputRedirectionTemplateValidator(String streamName) {
        this.streamName = streamName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Template template, HeterogeneousRegistry registry) {
        int count = ListUtils.count(new Predicate<DataObject>() {
            @Override
            public Boolean call(DataObject arg) {
                return StringUtils.equals(streamName, arg.getDataSourceName());
            }
        }, template.getOutputs());
        if (count > 1) {
            throw new TooManyOutputRedirectionsException(template.getName(), streamName);
        }
    }
}
