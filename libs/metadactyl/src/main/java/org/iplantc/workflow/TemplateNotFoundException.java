package org.iplantc.workflow;

/**
 * Thrown when a template can't be found.
 *
 * @author Dennis Roberts
 */
public class TemplateNotFoundException extends ElementNotFoundException {

    /**
     * @param templateId the template identifier.
     */
    public TemplateNotFoundException(String templateId) {
        super("template", templateId);
    }
}
