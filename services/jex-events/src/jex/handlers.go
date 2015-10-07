package main

import (
	"api"
	"bytes"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"messaging"
	"model"
	"net/http"
	"net/url"
	"path"
	"time"

	"github.com/streadway/amqp"
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

// HandleMessage handles a received message from the AMQP exchange.
func (p *PostEventHandler) HandleMessage(delivery amqp.Delivery) {
	body := delivery.Body
	delivery.Ack(false) //We're not doing batch deliveries, which is what the false means
	var event Event
	err := json.Unmarshal(body, &event)
	if err != nil {
		logger.Print(err)
		logger.Print(string(body[:]))
		return
	}

	// parse the body of the event that came from the amqp broker.
	event.Parse()
	logger.Println(event.String())

	// adds the job to the database, but only if it doesn't already exist.
	job, err := p.DB.AddJob(event.CondorID)
	if err != nil {
		logger.Printf("Error adding job: %s", err)
		return
	}

	// make sure the exit code is set so that it gets updated in upcoming steps.
	job.ExitCode = event.ExitCode

	// set the invocation id, but only if it's not set and the event actually
	// has a value to update it with.
	if job.InvocationID == "" && event.InvocationID != "" {
		job.InvocationID = event.InvocationID
		logger.Printf("Setting InvocationID to %s", job.InvocationID)
	} else {
		logger.Printf("Setting the InvocationID was not necessary")
	}

	// we're expecting an exit code of 0 for successful runs. HT jobs may have
	// more than one failure.
	if job.ExitCode != 0 {
		job.FailureCount = job.FailureCount + 1
	}

	// update the job with any additional information
	job, err = p.DB.UpdateJob(job) // Need to make sure the exit code gets stored.
	if err != nil {
		logger.Printf("Error updating job")
	}

	// update the parsed event object with info returned from the database.
	event.CondorID = job.CondorID
	event.InvocationID = job.InvocationID
	event.AppID = job.AppID
	event.User = job.Submitter

	// don't add the event to the database if it's already there.
	exists, err := p.DB.DoesCondorJobEventExist(event.Hash)
	if err != nil {
		logger.Printf("Error checking for job event existence by checksum: %s", event.Hash)
		return
	}
	if exists {
		logger.Printf("An event with a hash of %s already exists in the database, skipping", event.Hash)
		return
	}

	// send event updates upstream to Donkey
	err = p.Route(&event)
	if err != nil {
		logger.Printf("Error sending event upstream: %s", err)
	}

	// store the unparsed (raw), event information in the database.
	rawEventID, err := p.DB.AddCondorRawEvent(event.Event, job.ID)
	if err != nil {
		logger.Printf("Error adding raw event: %s", err)
		return
	}
	ce, err := p.DB.GetCondorEventByNumber(event.EventNumber)
	if err != nil {
		logger.Printf("Error getting condor event: %s", err)
		return
	}
	jobEventID, err := p.DB.AddCondorJobEvent(job.ID, ce.ID, rawEventID, event.Hash)
	if err != nil {
		logger.Printf("Error adding job event: %s", err)
		return
	}
	if p.ShouldUpdateLastEvents(&event) {
		_, err = p.DB.UpsertLastCondorJobEvent(jobEventID, job.ID)
		if err != nil {
			logger.Printf("Error upserting last condor job event: %s", err)
			return
		}
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

// CommandsHandler accepts deliveries from the jobs exchange sent with the
// jobs.commands routing key. Messages on that topic are meant to be requests
// to launch a job, though the purpose of the topic may expand in the future.
// Right now messages like that get forwarded to the jobs.launches topic.
type CommandsHandler struct {
	client *messaging.Client
	db     *Databaser
}

// NewCommandsHandler returns a newly instantiated *CommandsHandler.
func NewCommandsHandler(client *messaging.Client, db *Databaser) *CommandsHandler {
	return &CommandsHandler{
		client: client,
		db:     db,
	}
}

// Handle is the function that handles deliveries from the jobs.commands topic.
// All it does is forward them on to the jobs.launches topic.
func (c *CommandsHandler) Handle(delivery amqp.Delivery) {
	logger.Println("Received a jobs.commands delivery")
	body := delivery.Body
	delivery.Ack(false)
	cmd := &api.JobRequest{}
	err := json.Unmarshal(body, cmd)
	if err != nil {
		logger.Print(err)
		return
	}
	logger.Printf("Parsed the job request from the delivery. Invocation ID: %s\n", cmd.Job.InvocationID)
	uuid, err := c.db.InsertJob(cmd.Job)
	if err != nil {
		logger.Print(err)
		return
	}
	logger.Printf("Added job with invocation ID %s to database with ID of %s\n", cmd.Job.InvocationID, uuid)
	err = c.client.Publish(api.LaunchesKey, delivery.Body)
	if err != nil {
		logger.Print(err)
	}
}

// StopsHandler accepts deliveries from the jobs.stops.* topic of the jobs
// exchange and stores them in the database. No more, no less.
type StopsHandler struct {
	db *Databaser
}

// NewStopsHandler returns a newly instantiated *StopsHandler.
func NewStopsHandler(db *Databaser) *StopsHandler {
	return &StopsHandler{
		db: db,
	}
}

// Handle takes delivery of model.StopsRequests and stores them in the
// condor_job_stop_requests table of the jex database.
func (s *StopsHandler) Handle(delivery amqp.Delivery) {
	delivery.Ack(false)
	newStop := api.NewStopRequest()
	err := json.Unmarshal(delivery.Body, newStop)
	if err != nil {
		logger.Print(err)
		return
	}
	logger.Printf("Received stop request for %s\n", newStop.InvocationID)
	job, err := s.db.GetJobByInvocationID(newStop.InvocationID)
	if err != nil {
		logger.Print(err)
		return
	}
	logger.Printf("Found job record %s for stop request on %s\n", job.ID, newStop.InvocationID)
	dbStopRequest := &model.CondorJobStopRequest{
		JobID:         job.ID,
		Username:      newStop.Username,
		DateRequested: time.Now(),
		Reason:        newStop.Reason,
	}
	id, err := s.db.InsertCondorJobStopRequest(dbStopRequest)
	if err != nil {
		logger.Print(err)
	}
	logger.Printf("Added stop request %s for job %s\n", id, newStop.InvocationID)
}
