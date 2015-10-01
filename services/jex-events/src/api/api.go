package api

import (
	"bytes"
	"condor"
	"configurate"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"logcabin"
	"model"
	"net/http"
	"path"
	"strings"

	"github.com/gorilla/mux"
)

var (
	logger = logcabin.New()
)

// RespondWithError logs the error to stdout/stderr using msgTmpl as the template.
// The message is then written to 'w', after setting the status code to http.StatusBadRequest.
func RespondWithError(msgTmpl string, err error, w http.ResponseWriter) {
	msg := fmt.Sprintf(msgTmpl, err)
	logger.Println(msg)
	w.WriteHeader(http.StatusBadRequest)
	w.Write([]byte(msg))
	return
}

func rootHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "Welcome to the JEX.")
}

func submissionHandler(w http.ResponseWriter, r *http.Request) {
	b, err := ioutil.ReadAll(r.Body)
	if err != nil {
		RespondWithError("Error reading body:\n%s", err, w)
		return
	}
	s, err := model.NewFromData(b)
	if err != nil {
		RespondWithError("Error initializing submission:\n%s", err, w)
		return
	}
	sdir, err := condor.CreateSubmissionDirectory(s)
	if err != nil {
		RespondWithError("Error creating submission directory:\n%s", err, w)
		return
	}
	cmd, sh, err := condor.CreateSubmissionFiles(sdir, s)
	if err != nil {
		RespondWithError("Error creating submission files:\n%s", err, w)
		return
	}
	id, err := condor.Submit(cmd, sh, s)
	if err != nil {
		RespondWithError("Error submitting job:\n%s", err, w)
		return
	}
	logger.Printf("Condor job id is %s\n", id)
	m := make(map[string]string)
	m["sub_id"] = id
	marshalled, err := json.Marshal(m)
	if err != nil {
		RespondWithError("Error marshalling response:\n%s", err, w)
		return
	}
	_, err = w.Write(marshalled)
	if err != nil {
		logger.Printf("Error writing marshalled response:\n%s\n", err)
	}
	requestURL := path.Join(configurate.Config.JEXEvents, "jobs")
	if strings.HasSuffix(configurate.Config.JEXEvents, "/") {
		requestURL = fmt.Sprintf("%s%s", configurate.Config.JEXEvents, "jobs")
	} else {
		requestURL = fmt.Sprintf("%s/%s", configurate.Config.JEXEvents, "jobs")
	}
	record := &model.Job{
		CondorID:     id,
		AppID:        s.AppID,
		InvocationID: s.InvocationID,
		Submitter:    s.Submitter,
	}
	postBody, err := json.Marshal(record)
	if err != nil {
		log.Print(err)
	}
	logger.Printf(
		"Pushing to jex-events:\n\tCondorID: %s\n\tUsername: %s\n\tAppID: %s\n\tInvocationID: %s\n",
		record.CondorID,
		record.Submitter,
		record.AppID,
		record.InvocationID,
	)
	response, err := http.Post(requestURL, "application/json", bytes.NewBuffer(postBody))
	if err != nil {
		log.Print(err)
	}
	if response.StatusCode != 200 {
		log.Printf("POST to %s returned %d", requestURL, response.StatusCode)
	} else {
		respBody, err := ioutil.ReadAll(response.Body)
		if err != nil {
			log.Print(err)
		}
		logger.Printf("jex-events responded with a job id of %s\n", string(respBody))
	}
}

// params is what a command-line preview is parsed into.
type paramslist struct {
	P []model.StepParam `json:"params"`
}

func parameterPreview(w http.ResponseWriter, r *http.Request) {
	b, err := ioutil.ReadAll(r.Body)
	if err != nil {
		RespondWithError("Error reading body:\n%s", err, w)
		return
	}
	var params paramslist
	err = json.Unmarshal(b, &params)
	if err != nil {
		RespondWithError("Error unmarshalling request body:\n%s", err, w)
		return
	}
	w.Write([]byte(model.PreviewableStepParam(params.P).String()))
}

func stopHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	uuid := vars["uuid"]
	_, err := condor.Rm(uuid)
	if err != nil {
		RespondWithError("Error running 'condor_rm' for job:\n%s", err, w)
		return
	}
}

// Start initializes the http api for the JEX and gets it listening on the
// configured port
func Start(c *configurate.Configuration, l *logcabin.Lincoln) *mux.Router {
	router := mux.NewRouter()
	router.HandleFunc("/", rootHandler).Methods("GET")
	router.HandleFunc("/", submissionHandler).Methods("POST")
	router.HandleFunc("/arg-preview", parameterPreview).Methods("POST")
	router.HandleFunc("/stop/{uuid}", stopHandler).Methods("DELETE")
	return router
}
