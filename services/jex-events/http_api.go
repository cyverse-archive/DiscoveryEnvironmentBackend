package main

import (
	"encoding/json"
	"io/ioutil"
	"log"
	"net/http"
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

// Route looks at the requests method and decides which function should handle
// the request.
func (h *HTTPAPI) Route(writer http.ResponseWriter, request *http.Request) {
	switch request.Method {
	case "GET":
		h.JobHTTPGet(writer, request)
	case "POST":
		h.JobHTTPPost(writer, request)
	case "":
		h.JobHTTPGet(writer, request)
	default:
		log.Printf("Method %s is not supported on /jobs", request.Method)
	}
}

func (h *HTTPAPI) JobHTTPGet(writer http.ResponseWriter, request *http.Request) {

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
		WriteRequestError(writer, err.Error())
		return
	}
	var parsed JobRecord
	err = json.Unmarshal(bytes, &parsed)
	if err != nil {
		WriteRequestError(writer, err.Error())
		return
	}
	if parsed.Submitter == "" {
		WriteRequestError(writer, "The Submitter field is required in the POST JSON")
		return
	}
	if parsed.AppID == "" {
		WriteRequestError(writer, "The AppID field is required in the POST JSON")
		return
	}
	if parsed.CommandLine == "" {
		WriteRequestError(writer, "The CommandLine field is required in the POST JSON")
		return
	}
	if parsed.CondorID == "" {
		WriteRequestError(writer, "The CondorID field is required in the POST JSON")
		return
	}
	jobID, err := h.d.InsertJob(&parsed)
	if err != nil {
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(err.Error()))
		return
	}
	writer.Write([]byte(jobID))
}

// SetupHTTP configures a new HTTPAPI instance, registers handlers, and fires
// off a goroutinge that listens for requests. Should probably only be called
// once.
func SetupHTTP(config *Configuration, d *Databaser) {
	api := HTTPAPI{
		d: d,
	}
	http.HandleFunc("/jobs", api.Route)
	go func() {
		log.Fatal(http.ListenAndServe(config.HTTPListenPort, nil))
	}()
}
