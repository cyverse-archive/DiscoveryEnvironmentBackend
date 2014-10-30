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

func TestInsertGetUpdateDeleteRecord(t *testing.T) {
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
	newJR.DateCompleted = time.Now()
	updated, err := d.UpdateJob(newJR)
	if err != nil {
		t.Error(err)
	}
	if updated.DateCompleted.Format(time.RFC822Z) != newJR.DateCompleted.Format(time.RFC822Z) {
		t.Errorf("Updated date completed fields don't match")
	}
	err = d.DeleteJob(newUUID)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
}

func TestCRUDCondorEvents(t *testing.T) {
	connString := ConnString()
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	ce := &CondorEvent{
		EventNumber: 9001,
		EventName:   "test_event",
		EventDesc:   "event for unit tests",
	}
	newUUID, err := d.InsertCondorEvent(ce)
	if err != nil {
		t.Error(err)
	}
	ce.ID = newUUID
	getCE, err := d.GetCondorEvent(newUUID)
	if err != nil {
		t.Error(err)
	}
	if getCE.EventNumber != ce.EventNumber {
		t.Errorf("EventNumbers don't match")
	}
	if getCE.EventName != ce.EventName {
		t.Errorf("EventNames don't match")
	}
	if getCE.EventDesc != ce.EventDesc {
		t.Errorf("EventDescs don't match")
	}
	ce.EventNumber = 9002
	updated, err := d.UpdateCondorEvent(ce)
	if err != nil {
		t.Error(err)
	}
	if updated.EventNumber != ce.EventNumber {
		t.Errorf("EventNumbers don't match after update")
	}
	if updated.EventName != ce.EventName {
		t.Errorf("EventNames don't match after update")
	}
	if updated.EventDesc != ce.EventDesc {
		t.Errorf("EventDescs don't match after update")
	}
	err = d.DeleteCondorEvent(updated.ID)
	if err != nil {
		t.Error(err)
	}
}
