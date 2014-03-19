package org.iplantc.workflow.integration;

import static org.iplantc.workflow.integration.util.AnalysisImportUtils.findExistingAnalysis;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.iplantc.persistence.dto.user.User;
import org.iplantc.persistence.dto.workspace.Workspace;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.TransformationActivityDao;
import org.iplantc.workflow.data.InputOutputMap;
import org.iplantc.workflow.integration.json.TitoAnalysisUnmarshaller;
import org.iplantc.workflow.integration.util.HeterogeneousRegistry;
import org.iplantc.workflow.integration.util.JsonUtils;
import org.iplantc.workflow.integration.util.NullHeterogeneousRegistry;
import org.iplantc.workflow.service.WorkspaceInitializer;
import org.iplantc.workflow.template.groups.TemplateGroup;
import org.iplantc.workflow.util.ListUtils;
import org.iplantc.workflow.util.Predicate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Used to import analyses from JSON objects. Each analysis consists of an ID, name and description along with a list
 * of steps in the analysis and a list of input-to-output mappings. The format of the input is:
 *
 * <pre>
 * <code>
 * {   "analysis_id": &lt;analysis_id&gt;,
 *     "analysis_name": &lt;analysis_name&gt;,
 *     "description": &lt;analysis_description&gt;,
 *     "type": &lt;analysis_type&gt;,
 *     "steps": [
 *         {   "id": &lt;step_id&gt;,
 *             "name": &lt;step_name&gt;,
 *             "template_id": &lt;template_id&gt;,
 *             "description": &lt;step_description&gt;,
 *             "config": {
 *                 &lt;property_id_1&gt;: &lt;property_value_1&gt;,
 *                 &lt;property_id_2&gt;: &lt;property_value_2&gt;,
 *                 ...,
 *                 &lt;property_id_n&gt;: &lt;property_value_n&gt;
 *             }
 *         },
 *         ...
 *     ],
 *     "mappings": [
 *         {   "source_step": &lt;source_step_name&gt;,
 *            "target_step": &lt;target_step_name&gt;,
 *             "map": {
 *                 &lt;output_id_1&gt;: &lt;input_id_1&gt;,
 *                 &lt;output_id_2&gt;: &lt;input_id_2&gt;,
 *                 ...,
 *                 &lt;output_id_n&gt;: &lt;input_id_n&gt;
 *             }
 *         },
 *         ...
 *     ]
 * }
 * </code>
 * </pre>
 *
 * @author Dennis Roberts
 */
public class AnalysisImporter implements ObjectImporter, ObjectVetter<TransformationActivity> {

    /**
     * Used to flush Analysis saves to prevent transient relation exceptions.
     */
    private Session session;

    /**
     * Used to create data access objects.
     */
    private final DaoFactory daoFactory;

    /**
     * Used to retrieve or import a template group.
     */
    private final TemplateGroupImporter templateGroupImporter;

    /**
     * Used to implement a user's workspace.
     */
    private final WorkspaceInitializer workspaceInitializer;

    /**
     * The registry of named objects.
     */
    private HeterogeneousRegistry registry = new NullHeterogeneousRegistry();

    /**
     * Indicates what should be done if an existing analysis matches the one that's being imported.
     */
    private UpdateMode updateMode = UpdateMode.DEFAULT;

    /**
     * True if we should allow vetted analyses to be updated.
     */
    private final boolean updateVetted;

    /**
     * Enables replacement of existing analyses with the same name.
     */
    @Override
    public void enableReplacement() {
        setUpdateMode(UpdateMode.REPLACE);
    }

    /**
     * Disables replacement of existing analyses with the same name.
     */
    @Override
    public void disableReplacement() {
        setUpdateMode(UpdateMode.THROW);
    }

    /**
     * Instructs the importer to ignore attempts to replace existing analyses.
     */
    @Override
    public void ignoreReplacement() {
        setUpdateMode(UpdateMode.IGNORE);
    }

    /**
     * Explicitly sets the update mode for the importer.
     *
     * @param updateMode the new update mode.
     */
    @Override
    public void setUpdateMode(UpdateMode updateMode) {
        this.updateMode = updateMode;
    }

    /**
     * Initializes a new analysis importer instance.
     *
     * @param daoFactory the factory used to create data access objects.
     * @param TemplateGroupImporter used to import template groups.
     * @param WorkspaceInitializer used to initialize user's workspaces.
     */
    public AnalysisImporter(DaoFactory daoFactory, TemplateGroupImporter templateGroupImporter,
            WorkspaceInitializer workspaceInitializer) {
        this(daoFactory, templateGroupImporter, workspaceInitializer, false);
    }

    /**
     * Initializes a new analysis importer instance.
     *
     * @param daoFactory the factory used to create data access objects.
     * @param TemplateGroupImporter used to import template groups.
     * @param WorkspaceInitializer used to initialize user's workspaces.
     * @param updateVetted true if we should allow vetted analyses to be replaced.
     */
    public AnalysisImporter(DaoFactory daoFactory, TemplateGroupImporter templateGroupImporter,
            WorkspaceInitializer workspaceInitializer, boolean updateVetted) {
        this.daoFactory = daoFactory;
        this.templateGroupImporter = templateGroupImporter;
        this.workspaceInitializer = workspaceInitializer;
        this.updateVetted = updateVetted;
    }

    /**
     * Sets the registry of named objects.
     *
     * @param registry the new registry.
     */
    public void setRegistry(HeterogeneousRegistry registry) {
        this.registry = registry == null ? new NullHeterogeneousRegistry() : registry;
    }

    /**
     * Sets the current session.
     * 
     * @param session
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Determines if an Analysis has been vetted.
     *
     * @param username
     *  Fully qualified name of user.
     * @param analysis
     *  Analysis to check
     * @return
     *  True if the analysis is vetted, false otherwise.
     */
    @Override
    public boolean isObjectVetted(String username, TransformationActivity analysis) {
        final long workspaceId = getWorkspaceId(username);
        return ListUtils.any(new Predicate<TemplateGroup>() {
            @Override
            public Boolean call(TemplateGroup arg) {
                return workspaceId != arg.getWorkspaceId();
            }
        }, daoFactory.getTemplateGroupDao().findTemplateGroupsContainingAnalysis(analysis));
    }

    /**
     * Imports an analysis.
     *
     * @param json the JSON object to import.
     * @return the analysis ID.
     * @throws JSONException if the JSON object we receive is invalid.
     */
    @Override
    public String importObject(JSONObject json) throws JSONException {
        String analysisId = JsonUtils.nonEmptyOptString(json, null, "analysis_id", "id");
        String analysisName = json.optString("analysis_name");
        TitoAnalysisUnmarshaller unmarshaller = new TitoAnalysisUnmarshaller(daoFactory, registry);
        unmarshaller.setWorkspaceInitializer(workspaceInitializer);
        TransformationActivityDao analysisDao = daoFactory.getTransformationActivityDao();

        TransformationActivity analysis = unmarshaller.fromJson(json);
        TransformationActivity existingAnalysis = findExistingAnalysis(analysisDao, analysisId, analysisName);

        String username = getUsername(json, analysis);

        if (existingAnalysis == null) {
            initializeWorkspace(username);
            saveAnalysis(analysisDao, analysis);
            templateGroupImporter.addAnalysisToWorkspace(username, analysis);
        }
        else if (updateMode == UpdateMode.REPLACE) {
            if (updateVetted || !isObjectVetted(username, existingAnalysis)) {
                // An InputOutputMap can't be deleted in the same "flush" as its associated
                // TransformationSteps.
                // Delete old mappings first.
                existingAnalysis.getMappings().clear();
                analysisDao.save(existingAnalysis);
                if (session != null) {
                    session.flush();
                }

                // Delete old steps next.
                existingAnalysis.getSteps().clear();
                analysisDao.save(existingAnalysis);
                if (session != null) {
                    session.flush();
                }

                // Copy and save analysis.
                existingAnalysis.copy(analysis);
                saveAnalysis(analysisDao, existingAnalysis);
                analysis = existingAnalysis;
            }
            else {
                throw new VettedWorkflowObjectException("Cannot replace analysis: vetted analysis found.");
            }
        }
        else if (updateMode == UpdateMode.THROW) {
            throw new WorkflowException("a duplicate analysis was found and replacement is not enabled");
        }
        registry.add(TransformationActivity.class, analysis.getName(), analysis);
        return analysis.getId();
    }

    private void saveAnalysis(TransformationActivityDao analysisDao, TransformationActivity analysis) {
        // A new InputOutputMap can't be saved in the same "flush" as its associated TransformationSteps.
        List<InputOutputMap> mappings = new ArrayList<InputOutputMap>(analysis.getMappings());

        // Save analysis without new mappings first, so that new steps are not transient.
        analysis.getMappings().clear();
        analysisDao.save(analysis);
        if (session != null) {
            session.flush();
        }

        // Now save analysis with new mappings.
        analysis.getMappings().addAll(mappings);
        analysisDao.save(analysis);
    }

    /**
     * Imports a list of analyses.
     *
     * @param array the JSON array to import.
     * @return the list of analysis IDs.
     * @throws JSONException if the JSON array we receive is invalid.
     */
    @Override
    public List<String> importObjectList(JSONArray array) throws JSONException {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < array.length(); i++) {
            result.add(importObject(array.getJSONObject(i)));
        }
        return result;
    }

    /**
     * Initializes the user's workspace.
     *
     * @param username the fully qualified username.
     */
    private void initializeWorkspace(String username) {
        workspaceInitializer.initializeWorkspace(daoFactory, username);
    }

    /**
     * Gets the workspace identifier.
     *
     * @param username the name of the user.
     * @return the workspace ID.
     * @throws WorkflowException if the integrator's workspace isn't found.
     */
    private long getWorkspaceId(String username) throws WorkflowException {
        User user = daoFactory.getUserDao().findByUsername(username);
        Workspace workspace = daoFactory.getWorkspaceDao().findByUser(user);
        if (workspace == null) {
            throw new WorkflowException("workspace not found for " + username);
        }
        return workspace.getId();
    }

    /**
     * Gets the name of the user importing the analysis.
     *
     * @param json the JSON object representing the analysis being imported.
     * @param analysis the analysis being imported.
     * @return the fully qualified username.
     * @throws WorkflowException if the username isn't found.
     */
    private String getUsername(JSONObject json, TransformationActivity analysis) throws WorkflowException {
        String username = json.optString("full_username");
        if (StringUtils.isEmpty(username)) {
            username = analysis.getIntegrationDatum().getIntegratorEmail();
        }
        if (StringUtils.isEmpty(username)) {
            throw new WorkflowException("username not provided for analysis: " + analysis.getName());
        }
        return username;
    }
}
