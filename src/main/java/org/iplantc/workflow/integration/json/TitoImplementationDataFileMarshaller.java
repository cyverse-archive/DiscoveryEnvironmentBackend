/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.iplantc.workflow.integration.json;

import java.util.Set;
import net.sf.json.JSONArray;
import org.iplantc.persistence.dto.data.ImplementationDataFile;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Translates any object implementing the ImplemtationDataFile interface into
 * json.
 * 
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class TitoImplementationDataFileMarshaller {   
    /**
     * Translates a set of data files into json.
     * 
     * @param implementation
     *  The implementation json object.  Data files will be placed in this
     *  object's object graph under test.input_files and test.output_files.
     * @param dataFiles
     *  Set of data files to convert to json.
     * @throws JSONException 
     *  Thrown if something goes wrong translating the objects into json.
     */
    public void marshalDataFiles(JSONObject implementation, Set<? extends ImplementationDataFile> dataFiles) throws JSONException {
        JSONArray inputFiles = new JSONArray();
        JSONArray outputFiles = new JSONArray();
        
        for (ImplementationDataFile datafile : dataFiles) {
            if(datafile.isInputFile()) {
                inputFiles.add(datafile.getFilename());
            } else {
                outputFiles.add(datafile.getFilename());
            }
        }
        
        JSONObject testJsonObj = implementation.has("test") ? implementation.getJSONObject("test") : new JSONObject();
        testJsonObj.put("input_files", inputFiles);
        testJsonObj.put("output_files", outputFiles);
        implementation.put("test", testJsonObj);
    }
}
