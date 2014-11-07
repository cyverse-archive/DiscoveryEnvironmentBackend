package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
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
}

// Route decides which handling function an event should be passed along to and
// then invokes that function.
func (p *PostEventHandler) Route(event *Event) error {
	switch event.EventNumber {
	case "000":
		return p.Submitted(event)
	case "001":
		return p.Running(event)
	case "002":
		return p.Failed(event)
	case "004":
		return p.Failed(event)
	case "005":
		return p.Completed(event)
	case "009":
		return p.Failed(event)
	case "010":
		return p.Failed(event)
	case "012":
		return p.Failed(event)
	default:
		return p.Unrouted(event)
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
		log.Printf(fmtString, resp.Status, resp.Proto, resp.ContentLength, headersString, err.Error())
	} else {
		log.Printf(fmtString, resp.Status, resp.Proto, resp.ContentLength, headersString, string(body[:]))
	}
}

// Submitted handles the events with an EventNumber of "000". This will
// assemble the JSON and POST it to the /de-job endpoints. This does not
// require a completion date and the Status will be set to "Submitted."
func (p *PostEventHandler) Submitted(event *Event) error {
	js := JobStatus{
		Status: "Submitted",
		UUID:   event.InvocationID,
	}
	state := JobState{
		State: js,
	}
	json, err := json.Marshal(state)
	if err != nil {
		return err
	}
	log.Printf("Sending 'Submitted' event to %s: %s", p.PostURL, string(json))
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
	js := JobStatus{
		Status: "Running",
		UUID:   event.InvocationID,
	}
	state := JobState{
		State: js,
	}
	json, err := json.Marshal(state)
	if err != nil {
		return err
	}
	log.Printf("Sending 'Running' event to %s: %s", p.PostURL, string(json))
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
	now := fmt.Sprintf("%d", time.Now().UnixNano()/int64(time.Millisecond))
	js := JobStatus{
		Status:         "Failed",
		CompletionDate: now,
		UUID:           event.InvocationID,
	}
	state := JobState{
		State: js,
	}
	json, err := json.Marshal(state)
	if err != nil {
		return err
	}
	log.Printf("Sending 'Failed' event to %s: %s", p.PostURL, string(json))
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
	now := fmt.Sprintf("%d", time.Now().UnixNano()/int64(time.Millisecond))
	js := JobStatus{
		Status:         "Completed",
		CompletionDate: now,
		UUID:           event.InvocationID,
	}
	state := JobState{
		State: js,
	}
	json, err := json.Marshal(state)
	if err != nil {
		return err
	}
	log.Printf("Sending Completed' event to %s: %s", p.PostURL, string(json))
	resp, err := http.Post(p.PostURL, "application/json", bytes.NewBuffer(json))
	if err != nil {
		return err
	}
	LogResponse(resp)
	return nil
}

// Unrouted handles events that don't get forwarded to the users. Right now we
// just log them.
func (p *PostEventHandler) Unrouted(event *Event) error {
	log.Printf("Event %s is not being forwarded to the user: %s", event.EventNumber, event.Event)
	return nil
}
