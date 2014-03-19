package org.iplantc.workflow.integration;

import org.iplantc.persistence.dto.step.TransformationStep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.model.Template;
import org.iplantc.persistence.dto.transformation.Transformation;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.AnalysisGenerator.
 *
 * @author Dennis Roberts
 */
public class AnalysisGeneratorTest {

    /**
     * The analysis generator to use in each of the unit tests.
     */
    private AnalysisGenerator generator;

    /**
     * Initializes each of the unit tests.
     */
    @Before
    public void initialize() {
        generator = new AnalysisGenerator();
    }

    /**
     * Verifies that the anlaysis generator generates analyses correctly.
     */
    @Test
    public void shouldGenerateAnalysis() {
        Template template = new Template();
        template.setId("imdahammer");
        template.setName("hammer");
        template.setDescription("a hammer's a tool, right?");
        TransformationActivity analysis = generator.generateAnalysis(template);

        assertEquals("imdahammer", analysis.getId());
        assertEquals(template.getName(), analysis.getName());
        assertEquals(template.getDescription(), analysis.getDescription());
        assertEquals(1, analysis.getSteps().size());
        assertNotNull(analysis.getSteps().get(0));

        TransformationStep step = analysis.getSteps().get(0);
        assertTrue(step.getGuid().matches("[-0-9A-F]{36}"));
        assertEquals(template.getName(), step.getName());
        assertEquals(template.getDescription(), step.getDescription());
        assertNotNull(step.getTransformation());

        Transformation transformation = step.getTransformation();
        assertEquals("", transformation.getName());
        assertEquals("", transformation.getDescription());
        assertEquals(template.getId(), transformation.getTemplate_id());
    }

    /**
     * Verifies that a description is automatically generated for the analysis if the template description is empty.
     */
    @Test
    public void shouldGenerateAnalysisDescriptionForEmptyTemplateDescription() {
        Template template = new Template();
        template.setId("imdahammer");
        template.setName("hammer");
        template.setDescription("");
        TransformationActivity analysis = generator.generateAnalysis(template);
        assertEquals("", analysis.getDescription());
    }

    /**
     * Verifies that a description is automatically generated for the analysis if the template description is null.
     */
    @Test
    public void shouldGenerateAnalysisDescriptionForNullTemplateDescription() {
        Template template = new Template();
        template.setId("imdahammer");
        template.setName("hammer");
        TransformationActivity analysis = generator.generateAnalysis(template);
        assertEquals("", analysis.getDescription());
    }
}
