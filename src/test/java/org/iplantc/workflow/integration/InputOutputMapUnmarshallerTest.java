package org.iplantc.workflow.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.iplantc.persistence.dto.step.TransformationStep;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.integration.json.TitoInputOutputMapUnmarshaller;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.create.InputOutputMapUnmarshaller.
 * 
 * @author Dennis Roberts
 */
public class InputOutputMapUnmarshallerTest {

    /**
     * The first transformation step in the analysis.
     */
    private TransformationStep step1;

    /**
     * The second transformation step in the analysis.
     */
    private TransformationStep step2;

    /**
     * The analysis.
     */
    private TransformationActivity analysis;

    /**
     * The unmarshaller instance that's being tested.
     */
    private TitoInputOutputMapUnmarshaller unmarshaller;

    /**
     * Initializes each test.
     */
    @Before
    public void initialize() {
        step1 = createStep("foo");
        step2 = createStep("bar");
        analysis = createAnalysis();
        unmarshaller = new TitoInputOutputMapUnmarshaller(analysis);
    }

    /**
     * Creates an analysis.
     * 
     * @return the analysis.
     */
    private TransformationActivity createAnalysis() {
        TransformationActivity analysis = new TransformationActivity();
        analysis.setId("analysisid");
        analysis.setName("analysisname");
        analysis.setDescription("analysisdescription");
        analysis.addStep(step1);
        analysis.addStep(step2);
        return analysis;
    }

    /**
     * Creates a transformation step.
     * 
     * @param name the name of the transformation step.
     * @return the transformation step.
     */
    private TransformationStep createStep(String name) {
        TransformationStep step = new TransformationStep();
        step.setName(name);
        return step;
    }

    /**
     * Verifies that we can successfully unmarshall a mapping.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test
    public void shouldUnmarshallMapping() throws JSONException {
        String jsonString = "{   \"source_step\": \"foo\",\n"
            + "    \"target_step\": \"bar\",\n"
            + "    \"map\": {\n"
            + "        \"output1\": \"input1\",\n"
            + "        \"output2\": \"input2\"\n"
            + "    }\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        InputOutputMap mapping = unmarshaller.unmarshall(json);
        assertSame(step1, mapping.getSource());
        assertSame(step2, mapping.getTarget());
        Map<String, String> expected = new HashMap<String, String>();
        expected.put("output1", "input1");
        expected.put("output2", "input2");
        assertEquals(expected, mapping.getInput_output_relation());
    }

    /**
     * Verifies that an unknown source step name causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = WorkflowException.class)
    public void UnknownSourceStepShouldGenerateException() throws JSONException {
        String jsonString = "{   \"source_step\": \"oof\",\n"
            + "    \"target_step\": \"bar\",\n"
            + "    \"map\": {\n"
            + "        \"output1\": \"input1\",\n"
            + "        \"output2\": \"input2\"\n"
            + "    }\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        unmarshaller.unmarshall(json);
    }

    /**
     * Verifies that an unknown target step causes an exception to be thrown.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = WorkflowException.class)
    public void UnknownTargetStepShouldGenerateException() throws JSONException {
        String jsonString = "{   \"source_step\": \"foo\",\n"
            + "    \"target_step\": \"rab\",\n"
            + "    \"map\": {\n"
            + "        \"output1\": \"input1\",\n"
            + "        \"output2\": \"input2\"\n"
            + "    }\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        unmarshaller.unmarshall(json);
    }

    /**
     * Verifies that a missing source step generates an exception.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingSourceStepShouldGenerateException() throws JSONException {
        String jsonString = "{   \"target_step\": \"bar\",\n"
            + "    \"map\": {\n"
            + "        \"output1\": \"input1\",\n"
            + "        \"output2\": \"input2\"\n"
            + "    }\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        unmarshaller.unmarshall(json);
    }

    /**
     * Verifies that a missing target step generates an exception.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingTargetStepShouldGenerateException() throws JSONException {
        String jsonString = "{   \"source_step\": \"foo\",\n"
            + "    \"map\": {\n"
            + "        \"output1\": \"input1\",\n"
            + "        \"output2\": \"input2\"\n"
            + "    }\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        unmarshaller.unmarshall(json);
    }

    /**
     * Verifies that a missing map generates an exception.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test(expected = JSONException.class)
    public void missingMapShouldGenerateException() throws JSONException {
        String jsonString = "{   \"source_step\": \"foo\",\n"
            + "    \"target_step\": \"bar\",\n"
            + "}\n";
        JSONObject json = new JSONObject(jsonString);
        unmarshaller.unmarshall(json);
    }

    /**
     * Verifies that we can unmarshall a list of input/output maps.
     * 
     * @throws JSONException if the JSON object is invalid.
     */
    @Test
    public void shouldUnmarshallList() throws JSONException {
        String jsonString = "[\n"
            + "    {   \"source_step\": \"foo\",\n"
            + "        \"target_step\": \"bar\",\n"
            + "        \"map\": {\n"
            + "            \"output1\": \"input1\",\n"
            + "            \"output2\": \"input2\"\n"
            + "        }\n"
            + "    },\n"
            + "    {   \"source_step\": \"foo\",\n"
            + "        \"target_step\": \"bar\",\n"
            + "        \"map\": {\n"
            + "            \"output3\": \"input3\",\n"
            + "            \"output4\": \"input4\"\n"
            + "        }\n"
            + "    }\n"
            + "]\n";
        JSONArray array = new JSONArray(jsonString);
        List<InputOutputMap> mappings = unmarshaller.unmarshall(array);
        assertEquals(2, mappings.size());

        InputOutputMap mapping1 = mappings.get(0);
        assertSame(step1, mapping1.getSource());
        assertSame(step2, mapping1.getTarget());
        Map<String, String> expected1 = new HashMap<String, String>();
        expected1.put("output1", "input1");
        expected1.put("output2", "input2");
        assertEquals(expected1, mapping1.getInput_output_relation());

        InputOutputMap mapping2 = mappings.get(1);
        assertSame(step1, mapping2.getSource());
        assertSame(step2, mapping2.getTarget());
        Map<String, String> expected2 = new HashMap<String, String>();
        expected2.put("output3", "input3");
        expected2.put("output4", "input4");
        assertEquals(expected2, mapping2.getInput_output_relation());
    }
}
