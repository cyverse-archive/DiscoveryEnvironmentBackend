package org.iplantc.workflow.integration.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;

import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.create.ImportUtils
 *
 * @author Dennis Roberts
 */
public class ImportUtilsTest {

	/**
	 * Verifies that we can generate an identifier without a prefix.
	 */
	@Test
	public void testGenerateId() {
		assertTrue(ImportUtils.generateId().matches("[-0-9A-F]{36}"));
	}

	/**
	 * Verifies that we can obtain a specified identifier from a JSON object.
	 *
	 * @throws JSONException if an invalid field name is used.
	 */
	@Test
	public void testGetIdWithSpecifiedId() throws JSONException {
	    JSONObject json = new JSONObject();
	    json.put("id", "someid");
	    assertEquals("someid", ImportUtils.getId(json, "id"));
	}

	/**
	 * Verifies that an identifier will be generated if one isn't specified.
	 *
	 * @throws JSONException if an invalid field name is used.
	 */
	@Test
	public void testGetIdWithUnspecifiedId() throws JSONException {
 	    assertTrue(ImportUtils.getId(new JSONObject(), "id").matches("[-0-9A-F]{36}"));
	}
}
