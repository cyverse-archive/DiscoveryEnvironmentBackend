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
