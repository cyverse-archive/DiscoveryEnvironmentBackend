package jexapi

import (
	"configurate"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"logcabin"
	"net/http"
	"submissions"

	"github.com/gorilla/mux"
)

var (
	cfg    *configurate.Configuration
	logger *logcabin.Lincoln
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
	s, err := submissions.NewFromData(b)
	if err != nil {
		RespondWithError("Error initializing submission:\n%s", err, w)
		return
	}
	sdir, err := submissions.CreateSubmissionDirectory(s)
	if err != nil {
		RespondWithError("Error creating submission directory:\n%s", err, w)
		return
	}
	cmd, sh, err := submissions.CreateSubmissionFiles(sdir, s)
	if err != nil {
		RespondWithError("Error creating submission files:\n%s", err, w)
		return
	}
	id, err := submissions.CondorSubmit(cmd, sh, s)
	if err != nil {
		RespondWithError("Error submitting job:\n%s", err, w)
		return
	}
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
}

// params is what a command-line preview is parsed into.
type paramslist struct {
	P []submissions.StepParam `json:"params"`
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
	w.Write([]byte(submissions.PreviewableStepParam(params.P).String()))
}

func stopHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	uuid := vars["uuid"]
	_, err := submissions.CondorRm(uuid)
	if err != nil {
		RespondWithError("Error running 'condor_rm' for job:\n%s", err, w)
		return
	}
}

// Init initializes the configuration and the logger.
func Init(c *configurate.Configuration, l *logcabin.Lincoln) {
	cfg = c
	logger = l
}

// Start initializes the http api for the JEX and gets it listening on the
// configured port
func Start(c *configurate.Configuration, l *logcabin.Lincoln) *mux.Router {
	Init(c, l)
	router := mux.NewRouter()
	router.HandleFunc("/", rootHandler).Methods("GET")
	router.HandleFunc("/", submissionHandler).Methods("POST")
	router.HandleFunc("/arg-preview", parameterPreview).Methods("POST")
	router.HandleFunc("/stop/{uuid}", stopHandler).Methods("DELETE")
	return router
}
