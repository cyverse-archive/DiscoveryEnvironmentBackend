package org.iplantc.workflow.integration.validation;

import org.iplantc.workflow.data.DataObject;
import org.iplantc.workflow.model.Template;
import org.iplantc.workflow.util.UnitTestUtils;

/**
 * Utility  methods for unit testing validators.
 * 
 * @author Dennis Roberts
 */
public class ValidatorTestUtils {

    /**
     * Prevent instantiation.
     */
    private ValidatorTestUtils() {
    }

    /**
     * Creates a template with two data objects with the given source names.
     *
     * @param source1 the name of the first data source.
     * @param source2 the name of the second data source.
     * @return the template.
     */
    public static Template createTemplate(String source1, String source2) {
        Template template = new Template();
        template.addOutputObject(createDataObject(source1));
        template.addOutputObject(createDataObject(source2));
        return template;
    }

    /**
     * @param source the name of the data source.
     * @return the data object.
     */
    public static DataObject createDataObject(String source) {
        DataObject dataObject = new DataObject();
        dataObject.setDataSource(UnitTestUtils.createDataSource(source));
        return dataObject;
    }
}
