package org.iplantc.workflow.experiment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.iplantc.hibernate.util.HibernateAccessor;
import org.iplantc.workflow.AnalysisNotFoundException;
import org.iplantc.workflow.AppSubmissionException;
import org.iplantc.workflow.client.OsmClient;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.dao.DaoFactory;
import org.iplantc.workflow.dao.hibernate.HibernateDaoFactory;
import org.iplantc.workflow.experiment.util.JobConfigUtils;
import org.iplantc.workflow.service.UserService;
import org.iplantc.workflow.user.UserDetails;

/**
 * This is a stub class for executing experiments
 *
 * @author Juan Antonio Raygoza Garay
 */
public class ExperimentRunner extends HibernateAccessor {

    public static final String CONDOR_TYPE = "condor";

    private static final Logger LOG = Logger.getLogger(ExperimentRunner.class);

    private static final Logger JsonLogger = Logger.getLogger("JsonLogger");

    private UserService userService;

    private String executionUrl;

    private UrlAssembler urlAssembler;

    private OsmClient jobRequestOsmClient;

    private String irodsHome;

    public ExperimentRunner() {
    }

    public String runExperiment(JSONObject experiment) throws Exception {
        Session session = getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            String result = runExperiment(experiment, session);
            tx.commit();
            return result;
        }
        catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
        finally {
            if (session.isOpen()) {
                session.close();
            }
        }

    }

    private String runExperiment(JSONObject experiment, Session session) throws Exception {
        LOG.debug("Running experiment: " + experiment);
        JsonLogger.info("runExperiment received the following input: " + experiment.toString(2));

        try {
            UserDetails userDetails = userService.getCurrentUserDetails();
            DaoFactory daoFactory = new HibernateDaoFactory(session);

            TransformationActivity app = findAnalysis(daoFactory, experiment.getString("analysis_id"));
            if (app.isDisabled()) {
                throw new Exception(String.format(
                        "The App \"%1$s\" is disabled and cannot be run at this time (App ID %2$s).",
                        app.getName(), app.getId()));
            }

            JSONObject job = formatJobRequest(experiment, daoFactory, userDetails);
            storeJobSubmission(experiment, job.getString("uuid"));
            submitJob(job);
            return formatResponse(job);
        }
        catch (Exception ex) {
            LOG.error("Caught exception when processing", ex);
            throw new Exception("ExperimentRunner error: " + ex.getMessage(), ex);
        }
    }

    protected TransformationActivity findAnalysis(DaoFactory daoFactory, String id) {
        TransformationActivity analysis = daoFactory.getTransformationActivityDao().findById(id);
        if (analysis == null) {
            throw new AnalysisNotFoundException(id);
        }
        return analysis;
    }

    private String formatResponse(JSONObject job) {
        JSONObject json = new JSONObject();
        json.put("job_id", job.getString("uuid"));
        return json.toString();
    }

    private void storeJobSubmission(JSONObject experiment, String jobUuid) {
        JSONObject state = new JSONObject();
        state.put("jobUuid", jobUuid);
        state.put("experiment", JobConfigUtils.escapeJobConfig(experiment));
        String uuid = jobRequestOsmClient.save(state);
        if (LOG.isDebugEnabled()) {
            LOG.debug("job request stored for job " + jobUuid + " with object persistence uuid " + uuid);
        }
    }

    protected JSONObject formatJobRequest(JSONObject experiment, DaoFactory daoFactory,
            UserDetails userDetails) {
        JobNameUniquenessEnsurer jobNameUniquenessEnsurer = new TimestampJobNameUniquenessEnsurer();
        JobRequestFormatterFactory factory = new JobRequestFormatterFactory(daoFactory, urlAssembler,
                userDetails, jobNameUniquenessEnsurer, irodsHome);
        return factory.getFormatter(experiment).formatJobRequest();
    }

    protected String submitJob(JSONObject job) throws UnsupportedEncodingException, IOException {
        /**
         * send message *
         */
        HttpClient client = new DefaultHttpClient();
        client.getParams().setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
        LOG.debug("Execution url: " + executionUrl);
        HttpPost post = new HttpPost(executionUrl);
        LOG.debug("Job: " + job);

        JsonLogger.info("Returning from runExperiment with the following result: "
                + job.toString(2));
        post.setEntity(new StringEntity(job.toString(), "application/json", "UTF-8"));

        HttpResponse response = client.execute(post);
        int responseStatus = response.getStatusLine().getStatusCode();
        LOG.debug("Response status from HttpClient post: " + responseStatus);

        if ((responseStatus < 200) || (responseStatus > 299)) {
            throw new AppSubmissionException(responseStatus, job.toString(2));
        }

        return IOUtils.toString(response.getEntity().getContent());
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public String getExecutionUrl() {
        return executionUrl;
    }

    public void setExecutionUrl(String executionUrl) {
        this.executionUrl = executionUrl;
    }

    public void setUrlAssembler(UrlAssembler urlAssembler) {
        this.urlAssembler = urlAssembler;
    }

    public UrlAssembler getUrlAssembler() {
        return urlAssembler;
    }

    public void setJobRequestOsmClient(OsmClient jobRequestOsmClient) {
        this.jobRequestOsmClient = jobRequestOsmClient;
    }

    public OsmClient getJobRequestOsmClient() {
        return jobRequestOsmClient;
    }

    public void setIrodsHome(String irodsHome) {
        this.irodsHome = irodsHome;
    }

    public String getIrodsHome() {
        return irodsHome;
    }
}
