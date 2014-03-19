package org.iplantc.workflow.integration.json;

import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains common functionality when unmarshalling json with test file
 * data.
 * 
 * @param <T> 
 *  Type of object which holds a data file for the object being unmarshalled
 *  by the inheriting class.
 * 
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public abstract class AbstractTitoDataFileUnmarshaller<T> {   
    /**
     * Method used to unmarshall lists of individual data files from json.
     * @param jsonFiles
     *  Json representing the array of files
     * @param input
     *  Are these files input files(true) or output files(false)?
     * @param files
     *  Set of files to add to when adding new files.
     * @throws JSONException 
     */
    protected abstract void unmarshallDataFileList(JSONArray jsonFiles, boolean input, Set<T> files) throws JSONException;

    /**
     * Unmarshalls data files from a json object.
     * 
     * @param json
     *  Json to unmarhsall
     * @return
     *  Set of data files.
     * @throws JSONException 
     */
    public Set<T> unmarshallDataFiles(JSONObject json) throws JSONException {
        Set<T> files = new HashSet<T>();

        JSONObject implementation = json.getJSONObject("implementation");
        JSONObject test = implementation.getJSONObject("test");

        JSONArray inputFiles = test.optJSONArray("input_files");
        if (inputFiles != null) {
            unmarshallDataFileList(inputFiles, true, files);
        }

        JSONArray outputFiles = test.optJSONArray("output_files");
        if (outputFiles != null) {
            unmarshallDataFileList(outputFiles, false, files);
        }

        return files;
    }
}
