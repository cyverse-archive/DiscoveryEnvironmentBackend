package org.iplantc.workflow.service.util;

import java.util.Arrays;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.util.UnitTestUtils;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.service.util.PipelineAnalysisValidator.
 * 
 * @author Dennis Roberts
 */
public class PipelineAnalysisValidatorTest {
    
    /**
     * Verifies that we get an exception for an analysis with no steps.
     */
    @Test(expected = WorkflowException.class)
    public void testEmptyAnalysis() {
        TransformationActivity analysis = UnitTestUtils.createAnalysis("analysis");
        PipelineAnalysisValidator.validateAnalysis(analysis);
    }

    /**
     * Verifies that we get an exception for a multi-step analysis.
     */
    @Test(expected = WorkflowException.class)
    public void testMultistepAnalysis() {
        TransformationActivity analysis = UnitTestUtils.createAnalysis("analysis");
        analysis.addStep(new TransformationStep());
        analysis.addStep(new TransformationStep());
        PipelineAnalysisValidator.validateAnalysis(analysis);
    }

    /**
     * Verifies that a single step analysis passes validation.
     */
    @Test
    public void testSingleStepAnalysis() {
        TransformationActivity analysis = UnitTestUtils.createAnalysis("analysis");
        analysis.addStep(new TransformationStep());
        PipelineAnalysisValidator.validateAnalysis(analysis);
    }

    /**
     * Verifies that a fAPI job does not pass validation.
     */
    @Test(expected = WorkflowException.class)
    public void testFapiJob() {
        TransformationActivity analysis = UnitTestUtils.createAnalysis("analysis");
        analysis.addStep(new TransformationStep());
        analysis.setJobTypeNames(Arrays.asList("fAPI"));
        PipelineAnalysisValidator.validateAnalysis(analysis);
    }

    /**
     * Verifies that a job with mixed job types does not pass validation.  This step is not representative of what
     * an actual mixed analysis would look like because a mixed analysis would have multiple steps by definition.
     */
    @Test(expected = WorkflowException.class)
    public void testMixedJob() {
        TransformationActivity analysis = UnitTestUtils.createAnalysis("analysis");
        analysis.addStep(new TransformationStep());
        analysis.setJobTypeNames(Arrays.asList("executable", "fAPI"));
        PipelineAnalysisValidator.validateAnalysis(analysis);
    }

    /**
     * Verifies that a job with an unknown job type does not pass validation.
     */
    @Test(expected = WorkflowException.class)
    public void testUnknownJob() {
        TransformationActivity analysis = UnitTestUtils.createAnalysis("analysis");
        analysis.addStep(new TransformationStep());
        analysis.setJobTypeNames(Arrays.asList("foo"));
        PipelineAnalysisValidator.validateAnalysis(analysis);
    }

    /**
     * Verifies that the validator can validate an analysis using its ID and a data access object factory.
     */
    @Test
    public void testValidationById() {
        MockDaoFactory daoFactory = new MockDaoFactory();
        TransformationActivity analysis = UnitTestUtils.createAnalysis("analysis");
        analysis.addStep(new TransformationStep());
        daoFactory.getTransformationActivityDao().save(analysis);
        PipelineAnalysisValidator.validateAnalysis("analysisid", daoFactory);
    }

    /**
     * Verifies that the validator throws an exception for an unknown analysis ID.
     */
    @Test(expected = WorkflowException.class)
    public void testUnknownAnalysisId() {
        PipelineAnalysisValidator.validateAnalysis("unknownid", new MockDaoFactory());
    }
}
