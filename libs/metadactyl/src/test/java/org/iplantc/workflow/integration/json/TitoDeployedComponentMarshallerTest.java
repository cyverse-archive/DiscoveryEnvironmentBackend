package org.iplantc.workflow.integration.json;

import static org.junit.Assert.assertEquals;

import org.iplantc.persistence.dto.components.DeployedComponent;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.json.TitoDeployedComponentMarshaller.
 * 
 * @author Dennis Roberts
 */
public class TitoDeployedComponentMarshallerTest {

    /**
     * The deployed component marshaller to use in each of the unit tests.
     */
    private TitoDeployedComponentMarshaller marshaller;

    /**
     * Initializes each of the unit tests.
     */
    @Before
    public void initialize() {
        marshaller = new TitoDeployedComponentMarshaller();
    }

    /**
     * Verifies that the marshaller correctly marshals deployed component fields.
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldMarshalDeployedComponentFields() throws JSONException {
        JSONObject component = marshaller.toJson(createDeployedComponent());
        assertEquals("componentid", component.getString("id"));
        assertEquals("componentname", component.getString("name"));
        assertEquals("componentlocation", component.getString("location"));
        assertEquals("componenttype", component.getString("type"));
        assertEquals("componentdescription", component.getString("description"));
        assertEquals("componentversion", component.getString("version"));
        assertEquals("componentattribution", component.getString("attribution"));
    }

    /**
     * Creates a deployed component for testing.
     * 
     * @return the deployed component.
     */
    private DeployedComponent createDeployedComponent() {
        DeployedComponent component = new DeployedComponent();
        component.setId("componentid");
        component.setName("componentname");
        component.setLocation("componentlocation");
        component.setToolType(UnitTestUtils.createToolType("componenttype"));
        component.setDescription("componentdescription");
        component.setVersion("componentversion");
        component.setAttribution("componentattribution");

        component.setIntegrationDatum(UnitTestUtils.createIntegrationDatum());
        component.setDeployedComponentDataFiles(UnitTestUtils.createDeployedComponentDataFiles());

        return component;
    }
}
