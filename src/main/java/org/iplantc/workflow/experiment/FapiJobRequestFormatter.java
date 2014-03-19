package org.iplantc.workflow.experiment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.iplantc.persistence.dto.step.TransformationStep;
import org.iplantc.workflow.WorkflowException;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.experiment.dto.JobConstructor;
import org.iplantc.workflow.user.UserDetails;

/**
 * A job request formatter used to submit jobs to the foundational API.
 *
 * @author Dennis Roberts
 */
public class FapiJobRequestFormatter implements JobRequestFormatter {

    /**
     * The job submission type identifier.
     */
    private static final String JOB_TYPE = "condor";

    /**
     * The number of spaces to indent nested elements in logged JSON objects.
     */
    private static final int JSON_INDENT = 2;

    /**
     * Used to log error and informational messages.
     */
    private static final Logger LOG = Logger.getLogger(FapiJobRequestFormatter.class);

    /**
     * Used to create data access objects.
     */
    private final DaoFactory daoFactory;

    /**
     * The details of the user who submitted the job.
     */
    private final UserDetails userDetails;

    /**
     * The configuration of the experiment.
     */
    private final JSONObject experiment;

    /**
     * The object used to ensure job name uniqueness.
     */
    private final JobNameUniquenessEnsurer jobNameUniquenessEnsurer;

    /**
     * The path to the home directory in iRODS.
     */
    private final String irodsHome;

    /**
     * @param daoFactory the factory used to create data access objects.
     * @param userDetails the details of the user who submitted the job.
     * @param experiment the configuration of the experiment.
     * @param jobNameUniquenessEnsurer used to ensure that jobs are uniquely named.
     */
    public FapiJobRequestFormatter(DaoFactory daoFactory, UserDetails userDetails, JSONObject experiment,
        JobNameUniquenessEnsurer jobNameUniquenessEnsurer, String irodsHome)
    {
        this.daoFactory = daoFactory;
        this.userDetails = userDetails;
        this.experiment = experiment;
        this.jobNameUniquenessEnsurer = jobNameUniquenessEnsurer;
        this.irodsHome = irodsHome;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JSONObject formatJobRequest() {
        logJson("experiment", experiment);
        ensureJobNameUniqueness();
        TransformationActivity analysis = loadAnalysis(experiment.getString("analysis_id"));
        JSONObject job = createJobObject(analysis);
        job.put("steps", formatSteps(analysis));
        job.put("email", userDetails.getEmail());
        logJson("job submission", job);
        return job;
    }

    /**
     * Ensures that the job name is unique for the user.
     */
    private void ensureJobNameUniqueness() {
        String originalName = experiment.getString("name");
        String uniqueName = jobNameUniquenessEnsurer.ensureUniqueJobName(userDetails.getShortUsername(), originalName);
        if (!StringUtils.equals(originalName, uniqueName)) {
            experiment.remove("name");
            experiment.put("name", uniqueName);
            experiment.put("display_name", originalName);
        }
    }

    /**
     * Formats the steps in the given analysis.
     *
     * @param analysis the analysis.
     * @return the formatted list of steps.
     */
    private Object formatSteps(TransformationActivity analysis) {
        JSONArray steps = new JSONArray();
        Map<String, List<String>> propertyValues = new HashMap<String, List<String>>();
        for (TransformationStep step : analysis.getSteps()) {
            FapiStepFormatter formatter = new FapiStepFormatter(daoFactory, JOB_TYPE, userDetails.getShortUsername(),
                    experiment, analysis, step, propertyValues, irodsHome);
            steps.add(formatter.formatStep());
        }
        return steps;
    }

    /**
     * Logs a JSON object if debugging is enabled.
     *
     * @param description a brief description of the JSON object to log.
     * @param json the JSON object to log.
     */
    private void logJson(String description, JSONObject json) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(description + ": " + json.toString(JSON_INDENT));
        }
    }

    /**
     * Begins the creation of the JSON object that will be used to submit the job.
     *
     * @param analysis the analysis representing the job that will be submitted.
     * @return the new JSON object.
     */
    private JSONObject createJobObject(TransformationActivity analysis) {
        JobConstructor jobConstructor = new JobConstructor("submit", JOB_TYPE);
        jobConstructor.setExperimentJson(experiment);
        jobConstructor.setAnalysis(analysis);
        jobConstructor.setUsername(userDetails.getShortUsername());
        return jobConstructor.getJob().toJson();
    }

    /**
     * Loads the analysis with the given identifier.
     *
     * @param analysisId the analysis identifier.
     * @return the analysis.
     * @throws WorkflowException if the analysis can't be found.
     */
    private TransformationActivity loadAnalysis(String analysisId) throws WorkflowException {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(analysisId);
        if (analysis == null) {
            throw new WorkflowException("analysis " + analysisId + " not found");
        }
        return analysis;
    }
}
