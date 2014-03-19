package org.iplantc.workflow.integration.validation;

import org.iplantc.workflow.integration.util.NullHeterogeneousRegistry;
import org.iplantc.workflow.model.Template;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.validation.TemplateValidatorFactory.
 * 
 * @author Dennis Roberts
 */
public class TemplateValidatorFactoryTest {
    
    /**
     * Verifies that the default validator passes if there are no output data objects that are redirected from standard
     * output or standard error output.  This test passes if no exceptions are thrown.
     */
    @Test
    public void validationShouldPassIfNoRedirectionIsUsed() {
        Template template = ValidatorTestUtils.createTemplate("file", "file");
        TemplateValidatorFactory.createDefaultTemplateValidator().validate(template, new NullHeterogeneousRegistry());
    }
    
    /**
     * Verifies that the default validator passes if there is one output data object that is redirected from standard
     * error output and one that is redirected from standard output.  This test passes if no exceptions are thrown.
     */
    @Test
    public void validationShouldPassIfRedirectionIsUsedOnceForEachStream() {
        Template template = ValidatorTestUtils.createTemplate("stdout", "stderr");
        TemplateValidatorFactory.createDefaultTemplateValidator().validate(template, new NullHeterogeneousRegistry());
    }

    /**
     * Verifies that the default validator fails if there are two output objects redirected from standard output.
     */
    @Test(expected = TooManyOutputRedirectionsException.class)
    public void validationShouldFailIfRedirectionIsUsedTwiceForStdout() {
        Template template = ValidatorTestUtils.createTemplate("stdout", "stdout");
        TemplateValidatorFactory.createDefaultTemplateValidator().validate(template, new NullHeterogeneousRegistry());
    }

    /**
     * Verifies that the default validator fails if there are two output objects redirected from standard error output.
     */
    @Test(expected = TooManyOutputRedirectionsException.class)
    public void validationShouldFailIfRedirectionIsUsedTwiceForStderr() {
        Template template = ValidatorTestUtils.createTemplate("stderr", "stderr");
        TemplateValidatorFactory.createDefaultTemplateValidator().validate(template, new NullHeterogeneousRegistry());
    }
}
