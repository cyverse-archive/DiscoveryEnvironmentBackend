package org.iplantc.workflow.experiment.util;

import net.sf.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for org.iplantc.workflow.experiment.util.JobConfigUtils.
 *
 * @author Dennis Roberts
 */
public class JobConfigUtilsTest {

	/**
	 * Test of escapeJobConfig method, of class JobConfigUtils.
	 */
	@Test
	public void testEscapeJobConfig() {
		JSONObject experiment = JSONObject.fromObject("{\"config\":{\"1.2\":\"3.4\"}}");
		JSONObject expected = JSONObject.fromObject("{\"config\":{\"1&#46;2\":\"3.4\"}}");
		JSONObject actual = JobConfigUtils.escapeJobConfig(experiment);
		assertEquals(expected, actual);
	}

	/**
	 * Test of escapeJsonKeys method, of class JobConfigUtils.
	 */
	@Test
	public void testEscapeJsonKeys() {
		JSONObject orig = JSONObject.fromObject("{\"1.2.3\":\"3.2.1\"}");
		JSONObject expected = JSONObject.fromObject("{\"1&#46;2&#46;3\":\"3.2.1\"}");
		JSONObject actual = JobConfigUtils.escapeJsonKeys(orig);
		assertEquals(expected, actual);
	}

	/**
	 * Test of escapeJsonKey method, of class JobConfigUtils.
	 */
	@Test
	public void testEscapeJsonKey() {
		String key = "an & and a .";
		String expected = "an &#38; and a &#46;";
		String actual = JobConfigUtils.escapeJsonKey(key);
		assertEquals(expected, actual);
	}

	/**
	 * Test of unescapeJsonKeys method, of class JobConfigUtils.
	 */
	@Test
	public void testUnescapeJsonKeys() {
		JSONObject orig = JSONObject.fromObject("{\"1&#46;2\":\"3.4\"}");
		JSONObject expected = JSONObject.fromObject("{\"1.2\":\"3.4\"}");
		JSONObject actual = JobConfigUtils.unescapeJsonKeys(orig);
		assertEquals(expected, actual);
	}

	/**
	 * Test of unescapeJsonKey method, of class JobConfigUtils.
	 */
	@Test
	public void testUnescapeJsonKey() {
		String key = "an &#38; and a &#46;";
		String expected = "an & and a .";
		String actual = JobConfigUtils.unescapeJsonKey(key);
		assertEquals(expected, actual);
	}
}
