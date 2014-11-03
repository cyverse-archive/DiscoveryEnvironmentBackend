package main

import (
	"encoding/json"
	"io/ioutil"
	"log"
	"net/http"
)

// type JobJSON struct {
// 	BatchID          string
// 	Submitter        string
// 	AppID            string
// 	CommandLine      string
// 	EnvVariables     string
// 	FailureThreshold int64
// 	DateSubmitted    string
// 	Date
// }

type HTTPAPI struct {
	d *Databaser
}

func WriteRequestError(writer http.ResponseWriter, msg string) {
	writer.WriteHeader(http.StatusBadRequest)
	writer.Write([]byte(msg))
}

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
	jobID, err := h.d.InsertJob(&parsed)
	if err != nil {
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(err.Error()))
		return
	}
	writer.Write([]byte(jobID))
}

func SetupHTTP(config *Configuration, d *Databaser) {
	api := HTTPAPI{
		d: d,
	}
	http.HandleFunc("/jobs", api.Route)
	go func() {
		log.Fatal(http.ListenAndServe(config.HTTPListenPort, nil))
	}()
}
