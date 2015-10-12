package api

import (
	"fmt"
	"logcabin"
	"model"
	"net/http"
)

//Command is tells the receiver of a JobRequest which action to perform
type Command int

const (
	//Launch tells the receiver of a JobRequest to launch the job
	Launch Command = iota

	//Stop tells the receiver of a JobRequest to stop a job
	Stop
)

var (
	logger = logcabin.New()

	//LaunchCommand is the string used in LaunchCo
	LaunchCommand = "LAUNCH"

	//JobsExchange is the name of the exchange that job related info is passed around.
	JobsExchange = "jobs"

	//LaunchesKey is the routing/binding key for job launch request messages.
	LaunchesKey = "jobs.launches"

	//UpdatesKey is the routing/binding key for job update messages.
	UpdatesKey = "jobs.updates"

	//StopsKey is the routing/binding key for job stop request messages.
	StopsKey = "jobs.stops"

	//CommandsKey is the routing/binding key for job command messages.
	CommandsKey = "jobs.commands"
)

// JobState defines a valid state for a job.
type JobState string

var (
	//QueuedState is when a job is queued.
	QueuedState JobState = "Queued"

	//SubmittedState is when a job has been submitted.
	SubmittedState JobState = "Submitted"

	//RunningState is when a job is running.
	RunningState JobState = "Running"

	//SucceededState is when a job has successfully completed the required steps.
	SucceededState JobState = "Complete"

	//FailedState is when a job has failed. Duh.
	FailedState JobState = "Failed"
)

// StatusCode defines a valid exit code for a job.
type StatusCode int

const (
	// Success is the exit code used when the required commands execute correctly.
	Success StatusCode = iota

	// StatusDockerPullFailed is the exit code when a 'docker pull' fails.
	StatusDockerPullFailed

	// StatusDockerCreateFailed is the exit code when a 'docker create' fails.
	StatusDockerCreateFailed

	// StatusInputFailed is the exit code when an input download fails.
	StatusInputFailed

	// StatusStepFailed is the exit code when a step in the job fails.
	StatusStepFailed

	// StatusOutputFailed is the exit code when the output upload fails.
	StatusOutputFailed

	// StatusKilled is the exit code when the job is killed.
	StatusKilled
)

// JobRequest is a generic request type for job related requests.
type JobRequest struct {
	Job     *model.Job
	Command Command
	Message string
	Version int
}

// StopRequest contains the information needed to stop a job
type StopRequest struct {
	Reason       string
	Username     string
	Version      int
	InvocationID string
}

// UpdateMessage contains the information needed to broadcast a change in state
// for a job.
type UpdateMessage struct {
	Job     *model.Job
	Version int
	State   JobState
	Message string
}

// NewStopRequest returns a *JobRequest that has been constructed to be a
// stop request for a running job.
func NewStopRequest() *StopRequest {
	return &StopRequest{
		Version: 0,
	}
}

// NewLaunchRequest returns a *JobRequest that has been constructed to be a
// launch request for the provided job.
func NewLaunchRequest(j *model.Job) *JobRequest {
	return &JobRequest{
		Job:     j,
		Command: Launch,
		Version: 0,
	}
}

// RespondWithError logs the error to stdout/stderr using msgTmpl as the template.
// The message is then written to 'w', after setting the status code to http.StatusBadRequest.
func RespondWithError(msgTmpl string, err error, w http.ResponseWriter) {
	msg := fmt.Sprintf(msgTmpl, err)
	logger.Println(msg)
	w.WriteHeader(http.StatusBadRequest)
	w.Write([]byte(msg))
	return
}
