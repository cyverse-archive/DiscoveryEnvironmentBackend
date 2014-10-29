package main

import (
	"fmt"
	"os"
	"testing"
	"time"

	"code.google.com/p/go-uuid/uuid"
)

func ConnString() string {
	connString := os.Getenv("JEXDB")
	if connString == "" {
		fmt.Println("JEXDB environment variable must be set to a postgresql URL")
		os.Exit(-1)
	}
	return connString
}

func TestNewDatabaser(t *testing.T) {
	connString := ConnString()
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	if d == nil {
		t.Errorf("db with a connection url of %s was nil", connString)
	}
}

func TestInsertGetDeleteRecord(t *testing.T) {
	connString := ConnString()
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	submitted := time.Now()
	started := time.Now()
	completed := time.Now()
	jr := &JobRecord{
		BatchID:       "",
		Submitter:     "unit_tests",
		DateSubmitted: submitted,
		DateStarted:   started,
		DateCompleted: completed,
		AppID:         uuid.New(),
		CommandLine:   "this --is -a --test",
		EnvVariables:  "TEST=true",
	}
	newUUID, err := d.InsertJob(jr)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if newUUID == "" {
		t.Errorf("InsertJob returned an empty UUID.")
		t.Fail()
	}
	newJR, err := d.GetJob(newUUID)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if newJR.BatchID != jr.BatchID {
		t.Errorf("BatchIDs didn't match")
	}
	if newJR.Submitter != jr.Submitter {
		t.Errorf("Submitters didn't match")
	}
	if newJR.DateSubmitted.Format(time.RFC822Z) != jr.DateSubmitted.Format(time.RFC822Z) {
		t.Errorf("Submitted dates didn't match")
	}
	if newJR.DateStarted.Format(time.RFC822Z) != jr.DateStarted.Format(time.RFC822Z) {
		t.Errorf("DateStarteds didn't match")
	}
	if newJR.DateCompleted.Format(time.RFC822Z) != jr.DateCompleted.Format(time.RFC822Z) {
		t.Errorf("DateCompleteds didn't match")
	}
	if newJR.AppID != jr.AppID {
		t.Errorf("AppIDs didn't match")
	}
	if newJR.CommandLine != jr.CommandLine {
		t.Errorf("CommandLines didn't match")
	}
	if newJR.EnvVariables != jr.EnvVariables {
		t.Errorf("EnvVariables didn't match")
	}
	if newJR.ExitCode != jr.ExitCode {
		t.Errorf("ExitCodes didn't match")
	}
	if newJR.FailureThreshold != jr.FailureThreshold {
		t.Errorf("FailureThresholds didn't match")
	}
	if newJR.FailureCount != jr.FailureCount {
		t.Errorf("FailureCounts didn't match")
	}
	if newJR.ID != newUUID {
		t.Errorf("IDs didn't match")
	}

	err = d.DeleteJob(newUUID)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
}
