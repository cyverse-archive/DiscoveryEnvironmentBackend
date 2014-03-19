package org.iplantc.workflow.integration.validation;

import org.iplantc.workflow.integration.util.NullHeterogeneousRegistry;
import org.iplantc.workflow.model.Template;
import org.junit.Test;

import static org.iplantc.workflow.integration.validation.ValidatorTestUtils.createTemplate;

/**
 * Unit tests for org.iplantc.workflow.integration.validation.OutputRedirectionTemplateValidator.
 *
 * @author Dennis Roberts
 */
public class OutputRedirectionTemplateValidatorTest {

    /**
     * Verifies that the validation passes if no output data objects use redirection. This test passes if we don't get
     * any exceptions.
     */
    @Test
    public void shouldSucceedWithNoOutputRedirections() {
        Template template = createTemplate("file", "file");
        new OutputRedirectionTemplateValidator("stdout").validate(template, new NullHeterogeneousRegistry());
    }

    /**
     * Verifies that the validation passes if one output data object uses redirection. This test passes if we don't get
     * any exceptions.
     */
    @Test
    public void shouldSuccedWithOneOuptutRedirection() {
        Template template = createTemplate("stdout", "file");
        new OutputRedirectionTemplateValidator("stdout").validate(template, new NullHeterogeneousRegistry());
    }

    /**
     * Verifies that the validation fails if more than one output object uses redirection.
     */
    @Test(expected = TooManyOutputRedirectionsException.class)
    public void shouldFailWithTwoOutputRedirections() {
        Template template = createTemplate("stdout", "stdout");
        new OutputRedirectionTemplateValidator("stdout").validate(template, new NullHeterogeneousRegistry());
    }
}
