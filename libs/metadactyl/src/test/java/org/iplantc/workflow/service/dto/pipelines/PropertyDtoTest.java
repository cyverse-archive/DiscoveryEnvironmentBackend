package org.iplantc.workflow.service.dto.pipelines;

import org.iplantc.workflow.data.DataObject;
import net.sf.json.JSONObject;
import org.iplantc.workflow.model.Property;
import org.iplantc.workflow.util.UnitTestUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.service.dto.pipelines.PropertyDto.
 * 
 * @author Dennis Roberts
 */
public class PropertyDtoTest {

    /**
     * Verifies that we can construct a property DTO from a property.
     */
    @Test
    public void testConstructionFromProperty() {
        Property prop = createProperty();
        PropertyDto dto = new PropertyDto(prop);
        assertEquals(prop.getId(), dto.getId());
        assertEquals(prop.getName(), dto.getName());
        assertEquals(prop.getLabel(), dto.getLabel());
        assertEquals(prop.getDescription(), dto.getDescription());
        assertEquals(prop.getIsVisible(), dto.isVisible());
        assertEquals(prop.getDefaultValue(), dto.getDefaultValue());
        assertEquals(prop.getPropertyTypeName(), dto.getTypeName());
        assertNull(prop.getDataObject());
    }

    /**
     * Verifies that we can construct a property DTO from a property containing a data object.
     */
    @Test
    public void testConstructionFromPropertyWithDataObject() {
        Property prop = createProperty();
        prop.setDataObject(createDataObject());
        PropertyDto dto = new PropertyDto(prop);
        assertEquals(prop.getId(), dto.getId());
        assertEquals(prop.getName(), dto.getName());
        assertEquals(prop.getLabel(), dto.getLabel());
        assertEquals(prop.getDescription(), dto.getDescription());
        assertEquals(prop.getIsVisible(), dto.isVisible());
        assertEquals(prop.getDefaultValue(), dto.getDefaultValue());
        assertEquals(prop.getPropertyTypeName(), dto.getTypeName());
        assertEquals(new DataObjectDto(prop.getDataObject()), dto.getDataObject());
    }

    /**
     * Verifies that we can construct a property DTO from a JSON object.
     */
    @Test
    public void testConstructionFromJson() {
        JSONObject json = createJson();
        PropertyDto dto = new PropertyDto(json);
        assertEquals(json.getString("id"), dto.getId());
        assertEquals(json.getString("name"), dto.getName());
        assertEquals(json.getString("label"), dto.getLabel());
        assertEquals(json.getString("description"), dto.getDescription());
        assertEquals(json.getBoolean("isVisible"), dto.isVisible());
        assertEquals(json.getString("value"), dto.getDefaultValue());
        assertEquals(json.getString("type"), dto.getTypeName());
        assertFalse(json.has("data_object"));
    }

    /**
     * Verifies that we can construct a property DTO from a JSON object containing a data object.
     */
    @Test
    public void testConstructionFromJsonWithDataObject() {
        JSONObject json = createJson();
        json.put("data_object", new DataObjectDto(createDataObject()).toJson());
        PropertyDto dto = new PropertyDto(json);
        assertEquals(json.getString("id"), dto.getId());
        assertEquals(json.getString("name"), dto.getName());
        assertEquals(json.getString("label"), dto.getLabel());
        assertEquals(json.getString("description"), dto.getDescription());
        assertEquals(json.getBoolean("isVisible"), dto.isVisible());
        assertEquals(json.getString("value"), dto.getDefaultValue());
        assertEquals(json.getString("type"), dto.getTypeName());
        assertEquals(new DataObjectDto(json.getJSONObject("data_object")), dto.getDataObject());
    }

    /**
     * Verifies that we can construct a property DTO from a JSON string.
     */
    @Test
    public void testConstructionFromString() {
        JSONObject json = createJson();
        PropertyDto dto = new PropertyDto(json.toString());
        assertEquals(json.getString("id"), dto.getId());
        assertEquals(json.getString("name"), dto.getName());
        assertEquals(json.getString("label"), dto.getLabel());
        assertEquals(json.getString("description"), dto.getDescription());
        assertEquals(json.getBoolean("isVisible"), dto.isVisible());
        assertEquals(json.getString("value"), dto.getDefaultValue());
        assertEquals(json.getString("type"), dto.getTypeName());
        assertFalse(json.has("data_object"));
    }

    /**
     * Verifies that we can construct a property DTO from a JSON string containing a data object.
     */
    @Test
    public void testConstructionFromStringWithDataObject() {
        JSONObject json = createJson();
        json.put("data_object", new DataObjectDto(createDataObject()).toJson());
        PropertyDto dto = new PropertyDto(json.toString());
        assertEquals(json.getString("id"), dto.getId());
        assertEquals(json.getString("name"), dto.getName());
        assertEquals(json.getString("label"), dto.getLabel());
        assertEquals(json.getString("description"), dto.getDescription());
        assertEquals(json.getBoolean("isVisible"), dto.isVisible());
        assertEquals(json.getString("value"), dto.getDefaultValue());
        assertEquals(json.getString("type"), dto.getTypeName());
        assertEquals(new DataObjectDto(json.getJSONObject("data_object")), dto.getDataObject());
    }

    /**
     * Verifies that we can generate a DTO from an input data object.
     */
    @Test
    public void testFromInput() {
        DataObject dataObject = createDataObject();
        PropertyDto dto = PropertyDto.fromInput(dataObject);
        assertEquals(dataObject.getId(), dto.getId());
        assertEquals(dataObject.getSwitchString(), dto.getName());
        assertEquals(dataObject.getLabel(), dto.getLabel());
        assertEquals(dataObject.getDescription(), dto.getDescription());
        assertTrue(dto.isVisible());
        assertNull(dto.getDefaultValue());
        assertEquals("Input", dto.getTypeName());
        assertEquals(new DataObjectDto(dataObject), dto.getDataObject());
    }

    /**
     * Verifies that we can generate a DTO from an input data object with no label.
     */
    @Test
    public void testFromInputWithNoLabel() {
        DataObject dataObject = createDataObject();
        dataObject.setLabel(null);
        PropertyDto dto = PropertyDto.fromInput(dataObject);
        assertEquals(dataObject.getId(), dto.getId());
        assertEquals(dataObject.getSwitchString(), dto.getName());
        assertEquals(dataObject.getName(), dto.getLabel());
        assertEquals(dataObject.getDescription(), dto.getDescription());
        assertTrue(dto.isVisible());
        assertNull(dto.getDefaultValue());
        assertEquals("Input", dto.getTypeName());
        assertEquals(new DataObjectDto(dataObject), dto.getDataObject());
    }

    /**
     * Verifies that we can generate a DTO from an input data object with no name or label.
     */
    @Test
    public void testFromInputWithNoNameOrLabel() {
        DataObject dataObject = createDataObject();
        dataObject.setLabel(null);
        dataObject.setName(null);
        PropertyDto dto = PropertyDto.fromInput(dataObject);
        assertEquals(dataObject.getId(), dto.getId());
        assertEquals(dataObject.getSwitchString(), dto.getName());
        assertEquals(dataObject.getSwitchString(), dto.getLabel());
        assertEquals(dataObject.getDescription(), dto.getDescription());
        assertTrue(dto.isVisible());
        assertNull(dto.getDefaultValue());
        assertEquals("Input", dto.getTypeName());
        assertEquals(new DataObjectDto(dataObject), dto.getDataObject());
    }

    /**
     * Verifies that we can generate a DTO from an input data object with no name, label or switch string.
     */
    @Test
    public void testFromInputWithNoNameLabelOrSwitchString() {
        DataObject dataObject = createDataObject();
        dataObject.setLabel(null);
        dataObject.setName(null);
        dataObject.setSwitchString(null);
        PropertyDto dto = PropertyDto.fromInput(dataObject);
        assertEquals(dataObject.getId(), dto.getId());
        assertEquals("", dto.getName());
        assertEquals("", dto.getLabel());
        assertEquals(dataObject.getDescription(), dto.getDescription());
        assertTrue(dto.isVisible());
        assertNull(dto.getDefaultValue());
        assertEquals("Input", dto.getTypeName());
        assertEquals(new DataObjectDto(dataObject), dto.getDataObject());
    }

    /**
     * Verifies that we can generate a DTO from an output data object.
     */
    @Test
    public void testFromOutput() {
        DataObject dataObject = createDataObject();
        PropertyDto dto = PropertyDto.fromOutput(dataObject);
        assertEquals(dataObject.getId(), dto.getId());
        assertEquals(dataObject.getSwitchString(), dto.getName());
        assertEquals(dataObject.getLabel(), dto.getLabel());
        assertEquals(dataObject.getDescription(), dto.getDescription());
        assertTrue(dto.isVisible());
        assertNull(dto.getDefaultValue());
        assertEquals("Output", dto.getTypeName());
        assertEquals(new DataObjectDto(dataObject), dto.getDataObject());
    }

    /**
     * Verifies that we can generate JSON for a property DTO.
     */
    @Test
    public void testJsonGeneration() {
        JSONObject expected = createJson();
        JSONObject actual = new PropertyDto(expected).toJson();
        assertEquals(expected, actual);
    }

    /**
     * Creates a property for testing.
     *
     * @return the property.
     */
    private Property createProperty() {
        Property property = new Property();
        property.setId("propid");
        property.setName("propname");
        property.setLabel("proplabel");
        property.setDescription("propdesc");
        property.setIsVisible(true);
        property.setDefaultValue("propvalue");
        property.setPropertyType(UnitTestUtils.createPropertyType("proptype"));
        return property;
    }

    /**
     * Creates a JSON object representing a property DTO for testing.
     * 
     * @return the JSON object.
     */
    private JSONObject createJson() {
        JSONObject json = new JSONObject();
        json.put("id", "propid");
        json.put("name", "propname");
        json.put("label", "proplabel");
        json.put("description", "propdesc");
        json.put("isVisible", true);
        json.put("value", "propvalue");
        json.put("type", "proptype");
        return json;
    }

    /**
     * Creates a data object for testing.
     * 
     * @return the data object.
     */
    private DataObject createDataObject() {
        DataObject dataObject = new DataObject();
        dataObject.setId("doid");
        dataObject.setName("doname");
        dataObject.setMultiplicity(UnitTestUtils.createMultiplicity("single"));
        dataObject.setOrderd(27);
        dataObject.setLabel("dolabel");
        dataObject.setDescription("dodescription");
        dataObject.setInfoType(UnitTestUtils.createInfoType("dotype"));
        dataObject.setDataFormat(UnitTestUtils.createDataFormat("doformat"));
        dataObject.setSwitchString("-d");
        dataObject.setRequired(true);
        dataObject.setRetain(true);
        return dataObject;
    }
}
