package org.iplantc.workflow.experiment.dto;

import java.util.UUID;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

/**
 * Java bean representing a Job.
 *
 * @author Kris Healy <healyk@iplantcollaborative.org>
 */
public class JobDto {
  private String uuid;

  private String name;
  private String displayName;

  private String description;
  /** @todo Should this be an enum? */
  private String requestType;
  private String executionTarget;
  private boolean notify;

  private String workspaceId;
  private String username;

  private String analysisName;
  private String analysisId;
  private String analysisDescription;

  private String outputDir;
  private boolean createOutputSubdir;

  /**
   * Creates a new JobDto.
   */
  public JobDto() {
    uuid = UUID.randomUUID().toString().toUpperCase();
  }

  /**
   * Constructs the job with a given request type and executor type.
   *
   * @param requestType
   *  Request type for this job.
   * @param executionTarget
   *  Target where the job will be executed.
   */
  public JobDto(String requestType, String executionTarget) {
    this();

    this.requestType = requestType;
    this.executionTarget = executionTarget;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getExecutionTarget() {
    return executionTarget;
  }

  public void setExecutionTarget(String executionTarget) {
    this.executionTarget = executionTarget;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
      return displayName;
  }

  public void setDisplayName(String displayName) {
      this.displayName = displayName;
  }

  public boolean isNotify() {
    return notify;
  }

  public void setNotify(boolean notify) {
    this.notify = notify;
  }

  public String getRequestType() {
    return requestType;
  }

  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public void setWorkspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
  }

  public String getAnalysisDescription() {
    return analysisDescription;
  }

  public void setAnalysisDescription(String analysisDescription) {
    this.analysisDescription = analysisDescription;
  }

  public String getAnalysisId() {
    return analysisId;
  }

  public void setAnalysisId(String analysisId) {
    this.analysisId = analysisId;
  }

  public String getAnalysisName() {
    return analysisName;
  }

  public void setAnalysisName(String analysisName) {
    this.analysisName = analysisName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(String outputDir) {
    this.outputDir = outputDir;
  }

  public boolean getCreateOutputSubdir() {
      return createOutputSubdir;
  }

  public void setCreateOutputSubdir(boolean createOutputSubdir) {
      this.createOutputSubdir = createOutputSubdir;
  }

  public JSONObject toJson() {
    JsonConfig config = new JsonConfig();
    config.clearJavaPropertyNameProcessors();
    config.registerJsonPropertyNameProcessor(JobDto.class, new CamelCapsToUnderscores());
    return JSONObject.fromObject(this, config);
  }
}
