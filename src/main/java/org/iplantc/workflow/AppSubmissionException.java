package org.iplantc.workflow;

public class AppSubmissionException extends WorkflowException {

	public AppSubmissionException(int status, String errString) {
		super("Submission failed with a status of " + status + " for JSON: " + errString);
	}
}
