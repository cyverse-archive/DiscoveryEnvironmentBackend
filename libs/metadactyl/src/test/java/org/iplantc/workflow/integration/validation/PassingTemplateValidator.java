package org.iplantc.workflow.integration.validation;

import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.model.Template;

/**
 * A template validator that always passes.
 * 
 * @author Dennis Roberts
 */
public class PassingTemplateValidator implements TemplateValidator {

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate(Template template, HeterogeneousRegistry registry) {
    }
}
