package org.iplantc.workflow.experiment.dto;

import net.sf.json.JSONObject;
import org.iplantc.workflow.core.TransformationActivity;
import org.iplantc.workflow.util.SfJsonUtils;

/**
 * Helper class used to build a job.  Used to fill out parts of the Job object
 * with values from other objects.
 *
 * When this class is constructor it automatically creates a new Job
 * internally.  Use <code>set..()</code> methods to construct the Job.  These
 * will take the passed in objects and transfer values form them to the
 * actual underlying Job object.
 *
 * @see Job
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class JobConstructor {
  private JobDto job;

  /**
   * Constructs a new JobConstructor.  This will internally create a new
   * Job.
   *
   * @param requestType
   *  Job's request type
   * @param targetExecutor
   *  Job's target executor
   */
  public JobConstructor(String requestType, String targetExecutor) {
    job = new JobDto(requestType, targetExecutor);
  }

  /**
   * Sets the experiment json info on the internal Job.
   *
   * @param experimentJson
   */
  public void setExperimentJson(JSONObject experimentJson) {
    job.setName(experimentJson.getString("name"));
    job.setDisplayName(experimentJson.optString("display_name"));
    job.setDescription(experimentJson.optString("description", ""));
    job.setNotify(experimentJson.getBoolean("notify"));
    job.setWorkspaceId(experimentJson.getString("workspace_id"));
    job.setOutputDir(SfJsonUtils.optString(experimentJson, "", "outputDirectory", "output_dir"));
    job.setCreateOutputSubdir(experimentJson.optBoolean("create_output_subdir", true));
  }

  /**
   * Sets the Analysis variables on the Job object.
   *
   * @param transformationActivity
   *  Activity to pull the analysis data from.
   */
  public void setAnalysis(TransformationActivity transformationActivity) {
    job.setAnalysisDescription(transformationActivity.getDescription());
    job.setAnalysisId(transformationActivity.getId());
    job.setAnalysisName(transformationActivity.getName());
  }

  /**
   * Fills out the Job with the username.
   *
   * @param userName
   *  Username to place in the job.
   */
  public void setUsername(String username) {
    job.setUsername(username);
  }

  public JobDto getJob() {
    return job;
  }
}
