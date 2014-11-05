package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"path"
	"time"

	"code.google.com/p/go-uuid/uuid"
)

// HTTPAPI stores the state that all of the API functionality will need.
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
	log.Printf(
		"Method: %s\tFrom: %s\tTo: %s\t Log: %s",
		request.Method,
		request.RemoteAddr,
		request.RequestURI,
		msg,
	)
}

// Route looks at the requests method and decides which function should handle
// the request.
func (h *HTTPAPI) Route(writer http.ResponseWriter, request *http.Request) {
	LogAPIMsg(request, "Request received; routing")
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

// JobHTTPGet is responsible for retrieving a Job from the database and returning
// its record as a JSON object. The UUID for the job is extracted from the basename
// of the URL path and must be a valid UUID.
func (h *HTTPAPI) JobHTTPGet(writer http.ResponseWriter, request *http.Request) {
	log.Printf("Handling GET request for %s", request.URL.Path)
	baseName := path.Base(request.URL.Path)
	if baseName == "" {
		WriteRequestError(writer, "The path must contain a job UUID")
		return
	}
	log.Printf("Requested job UUID: %s", baseName)
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
	log.Println(marshalled)
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
	var parsed JobRecord
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

// SetupHTTP configures a new HTTPAPI instance, registers handlers, and fires
// off a goroutinge that listens for requests. Should probably only be called
// once.
func SetupHTTP(config *Configuration, d *Databaser) {
	go func() {
		api := HTTPAPI{
			d: d,
		}
		http.HandleFunc("/jobs/", api.Route)
		http.HandleFunc("/jobs", api.Route)
		log.Printf("Listening for HTTP requests on %s", config.HTTPListenPort)
		log.Fatal(http.ListenAndServe(config.HTTPListenPort, nil))
	}()
}
