package org.iplantc.workflow.integration.validation;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.integration.util.NullHeterogeneousRegistry;
import org.iplantc.workflow.model.Template;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.validation.ChainingTemplateValidator.
 * 
 * @author Dennis Roberts
 */
public class ChainingTemplateValidatorTest {

    /**
     * Verifies that the validation passes if there's no validation to do.  This test passes if no exceptions are
     * thrown.
     */
    @Test
    public void shouldPassIfNoValidationsAreDone() {
        new ChainingTemplateValidator().validate(new Template(), new NullHeterogeneousRegistry());
    }

    /**
     * Verifies that the validation passes if all of the sub-validators pass.  This test passes if no exceptions are
     * thrown.
     */
    @Test
    public void shouldPassIfAllValidationsPass() {
        ChainingTemplateValidator validator = new ChainingTemplateValidator();
        validator.addValidator(new PassingTemplateValidator());
        validator.addValidator(new PassingTemplateValidator());
        validator.validate(new Template(), new NullHeterogeneousRegistry());
    }

    /**
     * Verifies that the validation fails if at least one sub-validator fails.
     */
    @Test(expected = WorkflowException.class)
    public void shouldFailIfOneValidationFails() {
        ChainingTemplateValidator validator = new ChainingTemplateValidator();
        validator.addValidator(new PassingTemplateValidator());
        validator.addValidator(new FailingTemplateValidator());
        validator.validate(new Template(), new NullHeterogeneousRegistry());
    }
}
