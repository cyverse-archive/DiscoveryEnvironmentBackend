package org.iplantc.workflow.service.dto.pipelines;

import net.sf.json.JSONObject;
import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.util.UnitTestUtils;
import static org.iplantc.workflow.integration.json.TitoMultiplicityNames.titoMultiplicityName;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.service.dto.pipelines.DataObjectDto.
 * 
 * @author Dennis Roberts
 */
public class DataObjectDtoTest {

    /**
     * Verifies that we can construct a DTO from a data object.
     */
    @Test
    public void testCreationFromDataObject() {
        DataObject dataObject = createDataObject();
        DataObjectDto dto = new DataObjectDto(dataObject);
        assertEquals(dataObject.getId(), dto.getId());
        assertEquals(dataObject.getName(), dto.getName());
        assertEquals(titoMultiplicityName(dataObject), dto.getMultiplicity());
        assertEquals(dataObject.getOrderd(), dto.getOrder());
        assertEquals(dataObject.getInfoTypeName(), dto.getInfoType());
        assertEquals(dataObject.getDataFormatName(), dto.getFormat());
        assertEquals(dataObject.getDescription(), dto.getDescription());
        assertEquals(dataObject.isRequired(), dto.isRequired());
        assertEquals(dataObject.getRetain(), dto.isRetained());
        assertEquals(dataObject.getSwitchString(), dto.getCmdSwitch());
    }

    /**
     * Verifies that we can construct a DTO from a JSON object.
     */
    @Test
    public void testCreationFromJson() {
        JSONObject json = createJson();
        DataObjectDto dto = new DataObjectDto(json);
        assertEquals(json.getString("id"), dto.getId());
        assertEquals(json.getString("name"), dto.getName());
        assertEquals(json.getString("multiplicity"), dto.getMultiplicity());
        assertEquals(json.getInt("order"), dto.getOrder());
        assertEquals(json.getString("file_info_type"), dto.getInfoType());
        assertEquals(json.getString("format"), dto.getFormat());
        assertEquals(json.getString("description"), dto.getDescription());
        assertEquals(json.getBoolean("required"), dto.isRequired());
        assertEquals(json.getBoolean("retain"), dto.isRetained());
        assertEquals(json.getString("cmdSwitch"), dto.getCmdSwitch());
    }

    /**
     * Verifies that we can construct a DTO from a JSON string.
     */
    @Test
    public void testCreationFromString() {
        JSONObject json = createJson();
        DataObjectDto dto = new DataObjectDto(json.toString());
        assertEquals(json.getString("id"), dto.getId());
        assertEquals(json.getString("name"), dto.getName());
        assertEquals(json.getString("multiplicity"), dto.getMultiplicity());
        assertEquals(json.getInt("order"), dto.getOrder());
        assertEquals(json.getString("file_info_type"), dto.getInfoType());
        assertEquals(json.getString("format"), dto.getFormat());
        assertEquals(json.getString("description"), dto.getDescription());
        assertEquals(json.getBoolean("required"), dto.isRequired());
        assertEquals(json.getBoolean("retain"), dto.isRetained());
        assertEquals(json.getString("cmdSwitch"), dto.getCmdSwitch());
    }

    /**
     * Verifies that we can generate JSON for a DTO.
     */
    @Test
    public void testJsonGeneration() {
        JSONObject expected = createJson();
        JSONObject actual = new DataObjectDto(expected).toJson();
        assertEquals(expected, actual);
    }

    /**
     * Verifies that fromDataObject works correctly when we pass in a data object.
     */
    @Test
    public void testFromDataObject() {
        DataObject dataObject = createDataObject();
        DataObjectDto dto = DataObjectDto.fromDataObject(dataObject);
        assertEquals(dataObject.getId(), dto.getId());
        assertEquals(dataObject.getName(), dto.getName());
        assertEquals(titoMultiplicityName(dataObject), dto.getMultiplicity());
        assertEquals(dataObject.getOrderd(), dto.getOrder());
        assertEquals(dataObject.getInfoTypeName(), dto.getInfoType());
        assertEquals(dataObject.getDataFormatName(), dto.getFormat());
        assertEquals(dataObject.getDescription(), dto.getDescription());
        assertEquals(dataObject.isRequired(), dto.isRequired());
        assertEquals(dataObject.getRetain(), dto.isRetained());
        assertEquals(dataObject.getSwitchString(), dto.getCmdSwitch());
    }

    /**
     * Verifies that fromDataObject returns null if a null data object is passed to it.
     */
    @Test
    public void testFromDataObjectWithNullArg() {
        assertNull(DataObjectDto.fromDataObject(null));
    }

    /**
     * Creates a data object to use for testing.
     * 
     * @return the data object.
     */
    private DataObject createDataObject() {
        DataObject dataObject = new DataObject();
        dataObject.setId("doid");
        dataObject.setName("doname");
        dataObject.setMultiplicity(UnitTestUtils.createMultiplicity("single"));
        dataObject.setOrderd(27);
        dataObject.setInfoType(UnitTestUtils.createInfoType("dotype"));
        dataObject.setDataFormat(UnitTestUtils.createDataFormat("doformat"));
        dataObject.setDescription("dodescription");
        dataObject.setRequired(true);
        dataObject.setRetain(true);
        dataObject.setSwitchString("-d");
        return dataObject;
    }

    /**
     * Creates a JSON object representing a data object for testing.
     * 
     * @return the JSON object.
     */
    private JSONObject createJson() {
        JSONObject json = new JSONObject();
        json.put("id", "doid");
        json.put("name", "doname");
        json.put("multiplicity", "One");
        json.put("order", 27);
        json.put("file_info_type", "dotype");
        json.put("format", "doformat");
        json.put("description", "dodescription");
        json.put("required", true);
        json.put("retain", true);
        json.put("cmdSwitch", "-d");
        return json;
    }
}
