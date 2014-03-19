package org.iplantc.workflow.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.dao.mock.MockDaoFactory;
import org.iplantc.workflow.util.UnitTestUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for org.iplantc.workflow.integration.TemplateExporter.
 * 
 * @author Dennis Roberts
 */
public class TemplateExporterTest {

    /**
     * Used to obtain data access objects.
     */
    private MockDaoFactory daoFactory;

    /**
     * The exporter being tested.
     */
    private TemplateExporter exporter;

    /**
     * Initializes each of the unit tests.
     */
    @Before
    public void initialize() {
        initializeDaoFactory();
        exporter = new TemplateExporter(daoFactory);
    }

    /**
     * Initializes the DAO factory for each of the unit tests.
     */
    private void initializeDaoFactory() {
        daoFactory = new MockDaoFactory();
        daoFactory.getDeployedComponentDao().save(UnitTestUtils.createDeployedComponent("tc", "templatecomponent"));
        daoFactory.getTemplateDao().save(UnitTestUtils.createTemplate("template"));
    }

    /**
     * Verifies that the exporter can successfully export an existing template.
     * 
     * @throws JSONException if a JSON error occurs.
     */
    @Test
    public void shouldExportTemplate() throws JSONException {
        JSONObject template = exporter.exportTemplate("templateid");
        assertNotNull(template);
        assertEquals("templateid", template.getString("id"));
    }

    /**
     * Verifies that the exporter throws a workflow exception if someone tries to export an unknown template.
     */
    @Test(expected = WorkflowException.class)
    public void shouldThrowExceptionForUnknownTemplate() {
        exporter.exportTemplate("unknown");
    }

    /**
     * Verifies that the exporter throws a workflow exception if a null pointer is passed to the exportTemplate method.
     */
    @Test(expected = WorkflowException.class)
    public void shouldThrowExceptionForNullTemplateId() {
        exporter.exportTemplate(null);
    }
}
