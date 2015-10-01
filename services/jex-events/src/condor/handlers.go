package condor

import (
	"api"
	"bytes"
	"configurate"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"model"
	"net/http"
	"path"
	"strings"

	"github.com/gorilla/mux"
)

func rootHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "Welcome to the JEX.")
}

func submissionHandler(w http.ResponseWriter, r *http.Request) {
	b, err := ioutil.ReadAll(r.Body)
	if err != nil {
		api.RespondWithError("Error reading body:\n%s", err, w)
		return
	}
	s, err := model.NewFromData(b)
	if err != nil {
		api.RespondWithError("Error initializing submission:\n%s", err, w)
		return
	}
	sdir, err := CreateSubmissionDirectory(s)
	if err != nil {
		api.RespondWithError("Error creating submission directory:\n%s", err, w)
		return
	}
	cmd, sh, err := CreateSubmissionFiles(sdir, s)
	if err != nil {
		api.RespondWithError("Error creating submission files:\n%s", err, w)
		return
	}
	id, err := Submit(cmd, sh, s)
	if err != nil {
		api.RespondWithError("Error submitting job:\n%s", err, w)
		return
	}
	logger.Printf("Condor job id is %s\n", id)
	m := make(map[string]string)
	m["sub_id"] = id
	marshalled, err := json.Marshal(m)
	if err != nil {
		api.RespondWithError("Error marshalling response:\n%s", err, w)
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
		api.RespondWithError("Error reading body:\n%s", err, w)
		return
	}
	var params paramslist
	err = json.Unmarshal(b, &params)
	if err != nil {
		api.RespondWithError("Error unmarshalling request body:\n%s", err, w)
		return
	}
	w.Write([]byte(model.PreviewableStepParam(params.P).String()))
}

func stopHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	uuid := vars["uuid"]
	_, err := Rm(uuid)
	if err != nil {
		api.RespondWithError("Error running 'condor_rm' for job:\n%s", err, w)
		return
	}
}
