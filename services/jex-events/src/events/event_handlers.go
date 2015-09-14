package events

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"path"
	"time"
)

// JobStatus contains the actual status of a job, as required by the /de-job
// endpoint.
type JobStatus struct {
	Status         string `json:"status"`
	CompletionDate string `json:"completion_date,omitempty"`
	UUID           string `json:"uuid"`
}

// JobState is the wrapping object for JobStatus. Doesn't have any operational
// value here, it's included simply for compatibility with the upstream APIs.
type JobState struct {
	State JobStatus `json:"state"`
}

// PostEventHandler is a type that contains the functions that handle the
// different job states. Right now we map the Condor states to DE states here.
type PostEventHandler struct {
	PostURL string
	JEXURL  string
	DB      *Databaser
}

// Returns the routing function

// Route decides which handling function an event should be passed along to and
// then invokes that function.
func (p *PostEventHandler) Route(event *Event) error {
	switch event.EventNumber {
	case "000": //Job Submitted
		return p.Submitted(event)
	case "001": //Job running
		return p.Running(event)
	case "002": // error in executable
		return p.Failed(event)
	case "004": // job evicted
		return p.Failed(event)
	case "005": // job terminated
		return p.Completed(event)
	case "009": // job aborted
		return p.Failed(event)
	case "010": // job suspended
		return p.Failed(event)
	case "012": // job held
		return p.Held(event)
	default:
		return p.Unrouted(event)
	}
}

// ShouldUpdateLastEvents indicates whether or not an event should cause
// last_condor_job_events to be updated.
func (p *PostEventHandler) ShouldUpdateLastEvents(event *Event) bool {
	switch event.EventNumber {
	case "000": //Job Submitted
		return true
	case "001": //Job running
		return true
	case "002": // error in executable
		return true
	case "004": // job evicted
		return true
	case "005": // job terminated
		return true
	case "009": // job aborted
		return true
	case "010": // job suspended
		return true
	case "012": // job held
		return true
	default:
		return false
	}
}

// LogResponse logs a htt.Response.
func LogResponse(resp *http.Response) {
	fmtString := "%s\t%s\t%d\n%s\n%s"
	var headers bytes.Buffer
	resp.Header.Write(&headers)
	headersString := headers.String()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		logger.Printf(fmtString, resp.Status, resp.Proto, resp.ContentLength, headersString, err.Error())
	} else {
		logger.Printf(fmtString, resp.Status, resp.Proto, resp.ContentLength, headersString, string(body[:]))
	}
}

// JobStatusStatus returns the string that should be used in the Status field
// of a JobStatus instance.
func JobStatusStatus(event *Event) string {
	switch event.EventNumber {
	case "000": //Job Submitted
		return "Submitted"
	case "001": //Job running
		return "Running"
	case "002": // error in executable
		return "Failed"
	case "004": // job evicted
		return "Failed"
	case "005": // job terminated
		if event.IsFailure() {
			return "Failed"
		}
		return "Completed"
	case "009": // job aborted
		return "Failed"
	case "010": // job suspended
		return "Failed"
	case "012": // job held
		return "Failed"
	default:
		return "Running"
	}
}

// NewJobState returns a new instance of JobState populated with info from
// the passed in event and status string. Does not include a completion date.
func NewJobState(event *Event) JobState {
	js := JobStatus{
		Status: JobStatusStatus(event),
		UUID:   event.InvocationID,
	}
	if js.Status == "Failed" || js.Status == "Completed" {
		now := fmt.Sprintf("%d", time.Now().UnixNano()/int64(time.Millisecond))
		js.CompletionDate = now
	}
	state := JobState{
		State: js,
	}
	return state
}

// Submitted handles the events with an EventNumber of "000". This will
// assemble the JSON and POST it to the /de-job endpoints. This does not
// require a completion date and the Status will be set to "Submitted."
func (p *PostEventHandler) Submitted(event *Event) error {
	state := NewJobState(event)
	json, err := json.Marshal(state)
	if err != nil {
		return err
	}
	logger.Printf("Sending 'Submitted' event to %s: %s", p.PostURL, string(json))
	resp, err := http.Post(p.PostURL, "application/json", bytes.NewBuffer(json))
	if err != nil {
		return err
	}
	LogResponse(resp)
	return nil
}

// Running handles the events with an EventNumber "001". This function assembles the JSON
// and POSTs it to the /de-job endpoint. This does not require a completion date in the
// outgoing JSON. The "status" field will be set to "Running".
func (p *PostEventHandler) Running(event *Event) error {
	state := NewJobState(event)
	json, err := json.Marshal(state)
	if err != nil {
		return err
	}
	logger.Printf("Sending 'Running' event to %s: %s", p.PostURL, string(json))
	resp, err := http.Post(p.PostURL, "application/json", bytes.NewBuffer(json))
	if err != nil {
		return err
	}
	LogResponse(resp)
	return nil
}

// Failed handles all events that map to a job failure, which encompasses multiple
// event numbers.
func (p *PostEventHandler) Failed(event *Event) error {
	state := NewJobState(event)
	json, err := json.Marshal(state)
	if err != nil {
		return err
	}
	logger.Printf("Sending 'Failed' event to %s: %s", p.PostURL, string(json))
	resp, err := http.Post(p.PostURL, "application/json", bytes.NewBuffer(json))
	if err != nil {
		return err
	}
	LogResponse(resp)
	return nil

}

// Completed handles event number 005, which maps to a job completion. However,
// a completion does not necessarily mean that the job completed successfully,
// so this function does a bit more logic to figure out if the job finished or
// not.
func (p *PostEventHandler) Completed(event *Event) error {
	if event.IsFailure() {
		return p.Failed(event)
	}
	state := NewJobState(event)
	json, err := json.Marshal(state)
	if err != nil {
		return err
	}
	logger.Printf("Sending Completed' event to %s: %s", p.PostURL, string(json))
	resp, err := http.Post(p.PostURL, "application/json", bytes.NewBuffer(json))
	if err != nil {
		return err
	}
	LogResponse(resp)
	return nil
}

// Held handles event number 012, which means that a job was put into the held
// state. Our system can't recover from a job going into this state, so the job
// has to be killed. This is accomplished by sending a DELETE to a URL in the
// JEX. We don't have to send a status notification to Donkey here, since the
// job will come through as failed.
func (p *PostEventHandler) Held(event *Event) error {
	logger.Printf("Job %s is in the held state", event.ID)
	stopPath := path.Join("/stop", event.InvocationID)
	stopURL, err := url.Parse(p.JEXURL)
	if err != nil {
		return err
	}
	stopURL.Path = stopPath
	logger.Printf("Posting a request to stop job %s to %s", event.ID, stopURL.String())
	req, err := http.NewRequest("DELETE", stopURL.String(), nil)
	if err != nil {
		return nil
	}
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return err
	}
	LogResponse(resp)
	return nil
}

// Unrouted handles events that don't get forwarded to the users. Right now we
// just log them.
func (p *PostEventHandler) Unrouted(event *Event) error {
	logger.Printf("Event %s is not being forwarded to the user: %s", event.EventNumber, event.Event)
	return nil
}
