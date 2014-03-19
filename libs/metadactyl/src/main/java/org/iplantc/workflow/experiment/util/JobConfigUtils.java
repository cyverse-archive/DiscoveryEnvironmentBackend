package org.iplantc.workflow.experiment.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.json.JSONObject;

/**
 * Utility methods for working with job configurations.
 * 
 * @author Dennis Roberts
 */
public class JobConfigUtils {

	/**
	 * The regular expression pattern used to recognize escaped characters in JSON keys.
	 */
	private static final Pattern ESCAPED_CHAR_PATTERN = Pattern.compile("&#(\\d+);");

	/**
	 * A pattern that matches all of the characters that should be escaped.
	 */
	private static Pattern CHARS_TO_ESCAPE = Pattern.compile("[.&]");

	/**
	 * Escapes a job configuration object within an experiment.
	 * 
	 * @param experiment the experiment containing the job configuration object.
	 * @return a copy of the experiment with an escaped job configuration object.
	 */
	public static JSONObject escapeJobConfig(JSONObject experiment) {
		JSONObject result = JSONObject.fromObject(experiment.toString());
		result.put("config", escapeJsonKeys(experiment.getJSONObject("config")));
		return result;
	}

	/**
	 * Escapes the keys in a JSON object.
	 * 
	 * @param orig the original JSON object.
	 * @return a copy of the JSON object with the keys escaped.
	 */
	public static JSONObject escapeJsonKeys(JSONObject orig) {
		JSONObject result = new JSONObject();
		for (Object key: orig.keySet()) {
			result.put(escapeJsonKey(key.toString()), orig.get(key));
		}
		return result;
	}

	/**
	 * Escapes a single JSON key.
	 * 
	 * @param key the key to escape.
	 * @return the escaped key.
	 */
	public static String escapeJsonKey(String key) {
		StringBuffer result = new StringBuffer();
		Matcher matcher = CHARS_TO_ESCAPE.matcher(key);
		while (matcher.find()) {
			matcher.appendReplacement(result, "&#" + ((int) matcher.group().charAt(0)) + ";");
		}
		matcher.appendTail(result);
		return result.toString();
	}

	/**
	 * Unescapes the keys in a JSON object that was stored in the OSM.
	 *
	 * @param orig the original JSON object.
	 * @return a copy of the original JSON object with the keys unescaped.
	 */
	public static JSONObject unescapeJsonKeys(JSONObject orig) {
		JSONObject result = new JSONObject();
		for (Object key : orig.keySet()) {
			result.put(unescapeJsonKey(key.toString()), orig.get(key));
		}
		return result;
	}

	/**
	 * Unescapes a single JSON key.
	 *
	 * @param key the original key.
	 * @return the unescaped version of the key.
	 */
	public static String unescapeJsonKey(String key) {
		StringBuffer result = new StringBuffer();
		Matcher matcher = ESCAPED_CHAR_PATTERN.matcher(key);
		while (matcher.find()) {
			char[] chars = { (char) Integer.parseInt(matcher.group(1)) };
			matcher.appendReplacement(result, new String(chars));
		}
		matcher.appendTail(result);
		return result.toString();
	}

}
