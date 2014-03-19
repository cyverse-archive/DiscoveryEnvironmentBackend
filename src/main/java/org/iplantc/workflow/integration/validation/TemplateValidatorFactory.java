package org.iplantc.workflow.integration.validation;

/**
 * Used to create template validators for the input services.
 * 
 * @author Dennis Roberts
 */
public class TemplateValidatorFactory {

    /**
     * Prevent instantiation.
     */
    private TemplateValidatorFactory() {
    }

    /**
     * Creates the default template validator used by the import services.
     * 
     * @return the template validator.
     */
    public static TemplateValidator createDefaultTemplateValidator() {
        ChainingTemplateValidator validator = new ChainingTemplateValidator();
        validator.addValidator(new OutputRedirectionTemplateValidator("stdout"));
        validator.addValidator(new OutputRedirectionTemplateValidator("stderr"));
        return validator;
    }
}
