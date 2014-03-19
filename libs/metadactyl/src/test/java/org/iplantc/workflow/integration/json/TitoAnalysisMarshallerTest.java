package org.iplantc.workflow.integration.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.persistence.dto.transformation.Transformation;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.json.TitoAnalysisMarshaller.
 * 
 * @author Dennis Roberts
 */
public class TitoAnalysisMarshallerTest {

    /**
     * Used to obtain data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * The marshaller to use in all of the unit tests.
     */
    private TitoAnalysisMarshaller marshaller;

    /**
     * Initializes each of the unit tests.
     */
    @Before
    public void initialize() {
        initializeDaoFactory();
        marshaller = new TitoAnalysisMarshaller();
    }

    /**
     * Initializes the DAO factory for each of the unit tests.
     */
    private void initializeDaoFactory() {
        daoFactory = new MockDaoFactory();
        daoFactory.getTemplateDao().save(UnitTestUtils.createTemplate("firsttemplate"));
        daoFactory.getTemplateDao().save(UnitTestUtils.createTemplate("secondtemplate"));
    }

    /**
     * Verifies that the marshaller correctly marshals the analysis fields.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalAnalysisFields() throws JSONException {
        JSONObject analysis = marshaller.toJson(createAnalysis());
        assertEquals("analysisid", analysis.getString("analysis_id"));
        assertEquals("analysisname", analysis.getString("analysis_name"));
        assertEquals("analysisdescription", analysis.getString("description"));
        assertEquals("analysistype", analysis.getString("type"));
        assertEquals("analysiswikiurl", analysis.getString("wiki_url"));
        assertFalse(analysis.has("mappings"));
    }

    /**
     * Verifies that the marshaller correctly marshals the transformation and transformation step fields.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalTransformationSteps() throws JSONException {
        JSONObject analysis = marshaller.toJson(createAnalysis());
        assertTrue(analysis.has("steps"));

        JSONArray steps = analysis.getJSONArray("steps");
        assertEquals(1, steps.length());

        JSONObject step1 = steps.getJSONObject(0);
        assertEquals("firststepid", step1.getString("id"));
        assertEquals("firststepname", step1.getString("name"));
        assertEquals("firststepdescription", step1.getString("description"));
        assertEquals("firsttemplateid", step1.getString("template_id"));
    }

    /**
     * Verifies that the marshaller correctly marshals the property values that are stored in the transformation.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalPropertyValues() throws JSONException {
        TransformationActivity xformAnalysis = createAnalysis();
        JSONObject analysis = marshaller.toJson(xformAnalysis);
        assertTrue(analysis.has("steps"));

        JSONArray steps = analysis.getJSONArray("steps");
        assertEquals(1, steps.length());

        JSONObject step1 = steps.getJSONObject(0);
        assertTrue(step1.has("config"));

        JSONObject config1 = step1.getJSONObject("config");
        assertEquals(3, config1.length());
        assertEquals("oof", config1.get("foo"));
        assertEquals("rab", config1.get("bar"));
        assertEquals("zab", config1.get("baz"));
    }

    /**
     * Verifies that the marshaller correctly marshals multistep analyses.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalMultistepAnalysis() throws JSONException {
        JSONObject analysis = marshaller.toJson(createMultistepAnalysis());
        assertTrue(analysis.has("steps"));
        assertTrue(analysis.has("mappings"));

        JSONArray steps = analysis.getJSONArray("steps");
        assertEquals(2, steps.length());

        JSONObject step1 = steps.getJSONObject(0);
        assertEquals("firststepid", step1.getString("id"));
        assertEquals("firststepname", step1.getString("name"));
        assertEquals("firststepdescription", step1.getString("description"));
        assertEquals("firsttemplateid", step1.getString("template_id"));
        assertTrue(step1.has("config"));

        JSONObject config1 = step1.getJSONObject("config");
        assertEquals(3, config1.length());
        assertEquals("oof", config1.get("foo"));
        assertEquals("rab", config1.get("bar"));
        assertEquals("zab", config1.get("baz"));

        JSONObject step2 = steps.getJSONObject(1);
        assertEquals("secondstepid", step2.getString("id"));
        assertEquals("secondstepname", step2.getString("name"));
        assertEquals("secondstepdescription", step2.getString("description"));
        assertEquals("secondtemplateid", step2.getString("template_id"));
        assertTrue(step2.has("config"));

        JSONObject config2 = step2.getJSONObject("config");
        assertEquals(0, config2.length());

        JSONArray mappings = analysis.getJSONArray("mappings");
        assertEquals(1, mappings.length());

        JSONObject mapping = mappings.getJSONObject(0);
        assertEquals("firststepname", mapping.getString("source_step"));
        assertEquals("secondstepname", mapping.getString("target_step"));
        assertTrue(mapping.has("map"));

        JSONObject map = mapping.getJSONObject("map");
        assertEquals(2, map.length());
        assertEquals("secondstepinput", map.getString("firststepoutput"));
        assertEquals("secondstepfu", map.getString("firststepfu"));
    }

    /**
     * Verifies that the marshaller correctly uses backward references to templates if we tell it to.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldUseBackwardReferences() throws JSONException {
        JSONObject analysis = new TitoAnalysisMarshaller(daoFactory, true).toJson(createAnalysis());
        assertTrue(analysis.has("steps"));

        JSONArray steps = analysis.getJSONArray("steps");
        assertEquals(1, steps.length());

        JSONObject step = steps.getJSONObject(0);
        assertFalse(step.has("template_id"));
        assertEquals("firsttemplate", step.get("template_ref"));
    }

    /**
     * Verifies that the marshaller correctly uses backward references to templates in a multistep analysis if we tell
     * it to.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldUseBackwardReferencesInMultistepAnalysis() throws JSONException {
        JSONObject analysis = new TitoAnalysisMarshaller(daoFactory, true).toJson(createMultistepAnalysis());
        assertTrue(analysis.has("steps"));

        JSONArray steps = analysis.getJSONArray("steps");
        assertEquals(2, steps.length());

        JSONObject step1 = steps.getJSONObject(0);
        assertFalse(step1.has("template_id"));
        assertEquals("firsttemplate", step1.get("template_ref"));

        JSONObject step2 = steps.getJSONObject(1);
        assertFalse(step2.has("template_id"));
        assertEquals("secondtemplate", step2.get("template_ref"));
    }

    /**
     * Verifies that the marshaller throws an exception if it tries to get the name of an unknown template.
     */
    @Test(expected = WorkflowException.class)
    public void shouldGetExceptionForUnknownTemplateWhenReferencesAreEnabled() {
        TransformationActivity analysis = createAnalysis();
        analysis.getSteps().get(0).getTransformation().setTemplate_id("unknown");
        new TitoAnalysisMarshaller(daoFactory, true).toJson(analysis);
    }

    /**
     * Verifies that the marshaller does not throw an exception if the template is unknown and references are disabled.
     */
    @Test
    public void shouldNotGetExceptionForUnknownTemplateWhenReferencesAreDisabled() {
        TransformationActivity analysis = createAnalysis();
        analysis.getSteps().get(0).getTransformation().setTemplate_id("unknown");
        marshaller.toJson(analysis);
    }

    /**
     * Creates a multistep analysis to use for testing.
     * 
     * @return the analysis.
     */
    private TransformationActivity createMultistepAnalysis() {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setId("multistepanalysisid");
        analysis.setName("multistepanalysisname");
        analysis.setDescription("multistepanalysisdescription");
        analysis.setType("multistepanalysistype");
        analysis.setSteps(createTransformationStepsForMultistepAnalysis());
        
        analysis.setIntegrationDatum(UnitTestUtils.createIntegrationDatum());
        
        addMappingsToMultistepAnalysis(analysis);
        return analysis;
    }

    /**
     * Adds the input/output mappings to the multistep analysis. Note that the first two transformation steps must be
     * added to the analysis before calling this method.
     * 
     * @param analysis the analysis.
     */
    private void addMappingsToMultistepAnalysis(TransformationActivity analysis) {
        analysis.addMapping(createFirstMapping(analysis.getSteps().get(0), analysis.getSteps().get(1)));
    }

    /**
     * Creates the first mapping in the multistep analysis.
     * 
     * @param source the source transformation step.
     * @param target the target transformation step.
     * @return the mapping.
     */
    private InputOutputMap createFirstMapping(TransformationStep source, TransformationStep target) {
        InputOutputMap mapping = new InputOutputMap();
        mapping.setSource(source);
        mapping.setTarget(target);
        mapping.addAssociation("firststepoutput", "secondstepinput");
        mapping.addAssociation("firststepfu", "secondstepfu");
        return mapping;
    }

    /**
     * Creates the list of transformation steps for the multistep analysis.
     * 
     * @return the list of transformation steps.
     */
    private List<TransformationStep> createTransformationStepsForMultistepAnalysis() {
        List<TransformationStep> steps = new ArrayList<TransformationStep>();
        steps.add(createFirstTransformationStep());
        steps.add(createSecondTransformationStep());
        return steps;
    }

    /**
     * Creates the analysis to use for testing.
     * 
     * @return the analysis.
     */
    private TransformationActivity createAnalysis() {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setId("analysisid");
        analysis.setName("analysisname");
        analysis.setDescription("analysisdescription");
        analysis.setType("analysistype");
        analysis.setWikiurl("analysiswikiurl");
        analysis.setSteps(createTransformationSteps());
        analysis.setIntegrationDatum(UnitTestUtils.createIntegrationDatum());
        return analysis;
    }

    /**
     * Creates the list of transformation steps to use for testing.
     * 
     * @return the list of transformation steps.
     */
    private List<TransformationStep> createTransformationSteps() {
        List<TransformationStep> steps = new ArrayList<TransformationStep>();
        steps.add(createFirstTransformationStep());
        return steps;
    }

    /**
     * Creates the first transformation step in the list of transformation steps.
     * 
     * @return the transformation step.
     */
    private TransformationStep createFirstTransformationStep() {
        TransformationStep step = new TransformationStep();
        step.setGuid("firststepid");
        step.setName("firststepname");
        step.setDescription("firststepdescription");
        step.setTransformation(createTransformation("firsttemplateid", createFirstPropertyValues()));
        return step;
    }

    /**
     * Creates the property values for the first transformation.
     * 
     * @return the map of property values.
     */
    private Map<String, String> createFirstPropertyValues() {
        Map<String, String> propertyValues = new HashMap<String, String>();
        propertyValues.put("foo", "oof");
        propertyValues.put("bar", "rab");
        propertyValues.put("baz", "zab");
        return propertyValues;
    }

    /**
     * Creates the second transformation step in the list of transformation steps.
     * 
     * @return the transformation step.
     */
    private TransformationStep createSecondTransformationStep() {
        TransformationStep step = new TransformationStep();
        step.setGuid("secondstepid");
        step.setName("secondstepname");
        step.setDescription("secondstepdescription");
        step.setTransformation(createTransformation("secondtemplateid", createSecondPropertyValues()));
        return step;
    }

    /**
     * Creates the property values for the second transformation.
     * 
     * @return the map of property values.
     */
    private Map<String, String> createSecondPropertyValues() {
        return new HashMap<String, String>();
    }

    /**
     * Creates a transformation for the template with the given identifier.
     * 
     * @param templateId the template identifier.
     * @param the map of property names to property values.
     * @return the transformation.
     */
    private Transformation createTransformation(String templateId, Map<String, String> propertyValues) {
        Transformation transformation = new Transformation();
        transformation.setName("transformation_for_" + templateId);
        transformation.setTemplate_id(templateId);
        for(String key : propertyValues.keySet()) {
            transformation.addPropertyValue(key, propertyValues.get(key));
        }
        return transformation;
    }
}
