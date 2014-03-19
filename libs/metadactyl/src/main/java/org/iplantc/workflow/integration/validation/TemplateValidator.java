package org.iplantc.workflow.integration.validation;

import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.model.Template;

/**
 * Validates templates that are being imported into the DE.  This is implemented as an interface in order to enable
 * multiple validators to be chained together.
 * 
 * @author Dennis Roberts
 */
public interface TemplateValidator {

    /**
     * Validates a template.
     * 
     * @param template the template to validate.
     * @param registry used to look up elements that aren't in the database yet.
     */
    public void validate(Template template, HeterogeneousRegistry registry);
}
