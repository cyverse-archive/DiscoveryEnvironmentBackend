package events

import (
	"configurate"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"model"
	"net/http"
	"path"
	"strings"
	"time"

	"github.com/pborman/uuid"
)

// HTTPAPI encapsulates the HTTP+JSON API for jex-events. It provides access to
// to the database so that each endpoint does not have to set up its own
// connection.
type HTTPAPI struct {
	d *Databaser
}

// WriteRequestError writes out an error message to the writer and sets the
// the HTTP status to 400.
func WriteRequestError(writer http.ResponseWriter, msg string) {
	writer.WriteHeader(http.StatusBadRequest)
	writer.Write([]byte(msg))
}

// LogAPIMsg prints a message to the log with associated request info.
func LogAPIMsg(request *http.Request, msg string) {
	logger.Printf(
		"Method: %s\tFrom: %s\tTo: %s\t Log: %s",
		request.Method,
		request.RemoteAddr,
		request.RequestURI,
		msg,
	)
}

// RouteJobRequests routes requests to one of the Job-related handlers. It
// decides which function to call by examining the request method. If the
// request method somehow ends up being blank (which shouldn't happen), then
// the request is assumed to be a GET request. Right now only GETs and POSTs
// are supported.
func (h *HTTPAPI) RouteJobRequests(writer http.ResponseWriter, request *http.Request) {
	LogAPIMsg(request, "Job request received; routing")
	switch request.Method {
	case "GET":
		h.JobHTTPGet(writer, request)
	case "POST":
		h.JobHTTPPost(writer, request)
	case "":
		h.JobHTTPGet(writer, request)
	default:
		LogAPIMsg(request, fmt.Sprintf("Method %s is not supported on /jobs", request.Method))
	}
}

// RouteInvocationRequests routes requests to one of the Invocation-related
// handlers. Right now only GET requests are supported. If the request method
// somehow ends up being blank, the request is assumed to be a GET request.
func (h *HTTPAPI) RouteInvocationRequests(writer http.ResponseWriter, request *http.Request) {
	LogAPIMsg(request, "Invocation request received; routing")
	switch request.Method {
	case "GET":
		h.InvocationHTTPGet(writer, request)
	case "":
		h.InvocationHTTPGet(writer, request)
	default:
		LogAPIMsg(request, fmt.Sprintf("Method %s is not supported on /invocation", request.Method))
	}
}

// RouteLastEventRequests routes requests to one of the LastEvent-related handlers
// Only GET requests are supported. If the request method somehow ends up being
// blank, the request is assumed to be a GET request.
func (h *HTTPAPI) RouteLastEventRequests(writer http.ResponseWriter, request *http.Request) {
	LogAPIMsg(request, "Last event lookup request received; routing")
	switch request.Method {
	case "GET":
		h.LastEventHTTP(writer, request)
	case "":
		h.LastEventHTTP(writer, request)
	default:
		LogAPIMsg(request, fmt.Sprintf("Method %s is not supported for last event lookups", request.Method))
	}
}

// LastEventHTTP handles HTTP requests for looking up a job's last event. The
// job is looked up by its invocation ID. JSON is written to the response body
// in the following format:
//
// 		{
// 			"state" : {
// 				"uuid" : "",
// 				"status" : "",
// 				"completion_date" : ""
// 			}
//		}
//
// 'uuid' will be in the normal UUID format of 32 hex digits in 5 groups
//  delimited by '-'. For example: 'bf6ff4a0-7bcf-11e4-b116-123b93f75cba'.
//
// 'status' will be a one of 'Submitted', 'Running', 'Completed', or 'Failed'.
//
// 'completion_date' will be a timestamp that looks like
// '2006-01-02T15:04:05Z07:00'.
func (h *HTTPAPI) LastEventHTTP(writer http.ResponseWriter, request *http.Request) {
	logger.Printf("Handling GET request for %s", request.URL.Path)
	baseName := path.Base(request.URL.Path)
	if baseName == "" {
		WriteRequestError(writer, "The path must contain an invocation UUID")
		return
	}
	logger.Printf("Requested job UUID: %s", baseName)
	if uuid.Parse(baseName) == nil {
		WriteRequestError(writer, fmt.Sprintf("The base of the path must be a UUID: %s", baseName))
		return
	}
	jr, err := h.d.GetJobByInvocationID(baseName)
	if err != nil {
		WriteRequestError(writer, err.Error())
		return
	}
	if jr == nil {
		writer.WriteHeader(http.StatusNotFound)
		writer.Write([]byte(fmt.Sprintf("Job %s was not found", baseName)))
		return
	}
	lastCondorJobEvent, err := h.d.GetLastCondorJobEvent(jr.ID)
	if err != nil {
		writer.WriteHeader(http.StatusNotFound)
		writer.Write([]byte(fmt.Sprintf("Last event for job %s using invocation %s was not found", jr.ID, baseName)))
		return
	}
	lastJobEvent, err := h.d.GetCondorJobEvent(lastCondorJobEvent.CondorJobEventID)
	if err != nil {
		writer.WriteHeader(http.StatusNotFound)
		writer.Write([]byte(fmt.Sprintf("JobEvent %s was not found for last event lookup", lastCondorJobEvent.CondorJobEventID)))
		return
	}
	condorEvent, err := h.d.GetCondorEvent(lastJobEvent.CondorEventID)
	if err != nil {
		writer.WriteHeader(http.StatusNotFound)
		writer.Write([]byte(fmt.Sprintf("CondorEvent %s was not found for last event lookup", lastJobEvent.CondorEventID)))
		return
	}
	appEvent := &Event{
		EventNumber:  condorEvent.EventNumber,
		CondorID:     jr.CondorID,
		AppID:        jr.AppID,
		InvocationID: jr.InvocationID,
		User:         jr.Submitter,
		EventName:    condorEvent.EventName,
		ExitCode:     jr.ExitCode,
	}
	jobState := NewJobState(appEvent)
	marshalled, err := json.Marshal(jobState)
	if err != nil {
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(err.Error()))
		return
	}
	logger.Printf("Response for last event lookup by invocation %s:\n%s", baseName, string(marshalled[:]))
	writer.Write(marshalled)

}

// InvocationHTTPGet is responsible for getting a Job from the database by
// its InvocationID and returning the job record as a JSON encoded string body.
// The Invocation UUID is extracted from the basename of the URL path and must
// be a valid UUID.
func (h *HTTPAPI) InvocationHTTPGet(writer http.ResponseWriter, request *http.Request) {
	logger.Printf("Handling GET request for %s", request.URL.Path)
	baseName := path.Base(request.URL.Path)
	if baseName == "" {
		WriteRequestError(writer, "The path must contain an invocation UUID")
		return
	}
	logger.Printf("Requested job UUID: %s", baseName)
	if uuid.Parse(baseName) == nil {
		WriteRequestError(writer, fmt.Sprintf("The base of the path must be a UUID: %s", baseName))
		return
	}
	jr, err := h.d.GetJobByInvocationID(baseName)
	if err != nil {
		WriteRequestError(writer, err.Error())
		return
	}
	if jr == nil {
		writer.WriteHeader(http.StatusNotFound)
		writer.Write([]byte(fmt.Sprintf("Job %s was not found", baseName)))
		return
	}
	marshalled, err := json.Marshal(jr)
	if err != nil {
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(err.Error()))
		return
	}
	logger.Printf("Response for job lookup by invocation ID %s:\n%s", baseName, string(marshalled[:]))
	writer.Write(marshalled)
}

// JobHTTPGet is responsible for retrieving a Job from the database and returning
// its record as a JSON object. The UUID for the job is extracted from the basename
// of the URL path and must be a valid UUID.
func (h *HTTPAPI) JobHTTPGet(writer http.ResponseWriter, request *http.Request) {
	logger.Printf("Handling GET request for %s", request.URL.Path)
	baseName := path.Base(request.URL.Path)
	if baseName == "" {
		WriteRequestError(writer, "The path must contain a job UUID")
		return
	}
	logger.Printf("Requested job UUID: %s", baseName)
	if uuid.Parse(baseName) == nil {
		WriteRequestError(writer, fmt.Sprintf("The base of the path must be a UUID: %s", baseName))
		return
	}
	jr, err := h.d.GetJob(baseName)
	if err != nil {
		WriteRequestError(writer, err.Error())
		return
	}
	if jr == nil {
		writer.WriteHeader(http.StatusNotFound)
		writer.Write([]byte(fmt.Sprintf("Job %s was not found", baseName)))
		return
	}
	marshalled, err := json.Marshal(jr)
	if err != nil {
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(err.Error()))
		return
	}
	logger.Printf("Response for job lookup by UUID %s:\n%s", baseName, string(marshalled[:]))
	writer.Write(marshalled)
}

// JobHTTPPost is responsible for parsing JSON and inserting a new job into the
// database. The incoming JSON should have the following format:
//    {
//      "Submitter"   : "<string>",
//      "AppID"       : "<uuid>",
//      "CommandLine" : "<string>",
//      "CondorID"    : "<string>"
//    }
// Those are the required fields. The following fields are also accepted:
// 	  {
//      "BatchID"          : "<uuid>",
//      "DateSubmitted"    : "<timestamp>",
//      "DateStarted"      : "<timestamp>",
//      "DateCompleted"    : "<timestamp>",
//      "EnvVariables"     : "<string>",
//      "ExitCode"         : <int>,
//      "FailureCount"     : <int>,
//      "FailureThreshold" : <int>
//    }
// Any fields marked as <timestamp> must be a string formatted according to
// RFC3339. Here's an example from the Go programming language docs:
//    2006-01-02T15:04:05Z07:00
// The timezone *is* stored with dates, so you'll probably want to convert to
// UTC before sending the timestamps in the JSON.
func (h *HTTPAPI) JobHTTPPost(writer http.ResponseWriter, request *http.Request) {
	bytes, err := ioutil.ReadAll(request.Body)
	if err != nil {
		LogAPIMsg(request, err.Error())
		WriteRequestError(writer, err.Error())
		return
	}
	LogAPIMsg(request, fmt.Sprintf("%s", string(bytes)))
	var parsed model.JobRecord
	err = json.Unmarshal(bytes, &parsed)
	if err != nil {
		WriteRequestError(writer, err.Error())
		return
	}
	if parsed.Submitter == "" {
		errMsg := "The Submitter field is required in the POST JSON"
		LogAPIMsg(request, errMsg)
		WriteRequestError(writer, errMsg)
		return
	}
	if parsed.AppID == "" {
		errMsg := "The AppID field is required in the POST JSON"
		LogAPIMsg(request, errMsg)
		WriteRequestError(writer, errMsg)
		return
	}
	if parsed.CondorID == "" {
		errMsg := "The CondorID field is required in the POST JSON"
		LogAPIMsg(request, errMsg)
		WriteRequestError(writer, errMsg)
		return
	}
	// if parsed.InvocationID == "" {
	// 	errMsg := "The InvocationID field is required in the POST JSON"
	// 	LogAPIMsg(request, errMsg)
	// 	WriteRequestError(writer, errMsg)
	// 	return
	// }
	parsed.InvocationID = ""

	if parsed.DateSubmitted.IsZero() {
		parsed.DateSubmitted = time.Now()
	}
	if parsed.DateCompleted.IsZero() {
		parsed.DateCompleted = time.Now()
	}
	if parsed.DateStarted.IsZero() {
		parsed.DateStarted = time.Now()
	}
	job, err := h.d.UpsertJob(&parsed)
	if err != nil {
		errMsg := fmt.Sprintf("Error upserting job: %s", err)
		LogAPIMsg(request, errMsg)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(err.Error()))
		return
	}
	writer.Write([]byte(job.ID))
}

func formatPort(port string) string {
	if strings.HasPrefix(port, ":") {
		return port
	}
	return fmt.Sprintf(":%s", port)
}

// SetupHTTP configures a new HTTPAPI instance, registers handlers, and fires
// off a goroutinge that listens for requests. Should probably only be called
// once.
func SetupHTTP(config *configurate.Configuration, d *Databaser) {
	go func() {
		api := HTTPAPI{
			d: d,
		}
		http.HandleFunc("/jobs/", api.RouteJobRequests)
		http.HandleFunc("/jobs", api.RouteJobRequests)
		http.HandleFunc("/invocations/", api.RouteInvocationRequests)
		http.HandleFunc("/invocations", api.RouteInvocationRequests)
		http.HandleFunc("/last-events/", api.RouteLastEventRequests)
		http.HandleFunc("/last-events", api.RouteLastEventRequests)
		logger.Printf("Listening for HTTP requests on %s", config.HTTPListenPort)
		logger.Fatal(http.ListenAndServe(formatPort(config.HTTPListenPort), nil))
	}()
}
