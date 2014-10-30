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

func TestCRUDCondorRawEvents(t *testing.T) {
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
	jr.ID = newUUID

	ce := &CondorRawEvent{
		JobID:         jr.ID,
		EventText:     "this is a unit test event",
		DateTriggered: time.Now(),
	}
	rawEventID, err := d.InsertCondorRawEvent(ce)
	if err != nil {
		t.Error(err)
	}
	ce.ID = rawEventID
	rawEvent, err := d.GetCondorRawEvent(rawEventID)
	if err != nil {
		t.Error(err)
	}
	if rawEvent.EventText != ce.EventText {
		t.Errorf("EventTexts don't match")
	}
	if rawEvent.DateTriggered.Format(time.RFC822Z) != ce.DateTriggered.Format(time.RFC822Z) {
		t.Errorf("EventDescs don't match")
	}
	if rawEvent.JobID != ce.JobID {
		t.Errorf("JobIDs don't match")
	}
	ce.EventText = "another unit test text"
	updated, err := d.UpdateCondorRawEvent(ce)
	if err != nil {
		t.Error(err)
	}
	if updated.EventText != ce.EventText {
		t.Errorf("EventTexts don't match after update")
	}
	if updated.DateTriggered.Format(time.RFC822Z) != ce.DateTriggered.Format(time.RFC822Z) {
		t.Errorf("EventDescs don't match after update")
	}
	if updated.JobID != ce.JobID {
		t.Errorf("JobIDs don't match after update")
	}
	if updated.ID != ce.ID {
		t.Errorf("IDs don't match after update")
	}
	err = d.DeleteCondorRawEvent(rawEventID)
	if err != nil {
		t.Error(err)
	}
}

func TestCRUDCondorJobEvent(t *testing.T) {
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
	jobID, err := d.InsertJob(jr)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if jobID == "" {
		t.Errorf("InsertJob returned an empty UUID.")
		t.Fail()
	}
	jr.ID = jobID
	ce := &CondorEvent{
		EventNumber: 9001,
		EventName:   "test_event",
		EventDesc:   "event for unit tests",
	}
	eventID, err := d.InsertCondorEvent(ce)
	if err != nil {
		t.Error(err)
	}
	ce.ID = eventID
	cr := &CondorRawEvent{
		JobID:         jr.ID,
		EventText:     "this is a unit test event",
		DateTriggered: time.Now(),
	}
	rawEventID, err := d.InsertCondorRawEvent(cr)
	if err != nil {
		t.Error(err)
	}
	cr.ID = rawEventID
	cje := &CondorJobEvent{
		JobID:            jr.ID,
		CondorEventID:    ce.ID,
		CondorRawEventID: cr.ID,
		DateTriggered:    time.Now(),
	}
	jobEventUUID, err := d.InsertCondorJobEvent(cje)
	if err != nil {
		t.Error(err)
	}
	cje.ID = jobEventUUID
	jobEvent, err := d.GetCondorJobEvent(jobEventUUID)
	if err != nil {
		t.Error(err)
	}
	if jobEvent.ID != cje.ID {
		t.Errorf("IDs don't match")
	}
	if jobEvent.JobID != cje.JobID {
		t.Errorf("JobIDs don't match")
	}
	if jobEvent.CondorEventID != cje.CondorEventID {
		t.Errorf("CondorEventIDs don't match")
	}
	if jobEvent.CondorRawEventID != cje.CondorRawEventID {
		t.Errorf("CondorRawEventIDs don't match")
	}
	if jobEvent.DateTriggered.Format(time.RFC822Z) != cje.DateTriggered.Format(time.RFC822Z) {
		t.Errorf("DateTriggereds don't match")
	}
	cje.DateTriggered = time.Now()
	updatedCJE, err := d.UpdateCondorJobEvent(cje)
	if err != nil {
		t.Error(err)
	}
	if updatedCJE.ID != cje.ID {
		t.Errorf("IDs don't match after update")
	}
	if updatedCJE.JobID != cje.JobID {
		t.Errorf("JobIDs don't match after update")
	}
	if updatedCJE.CondorEventID != cje.CondorEventID {
		t.Errorf("CondorEventIDs don't match after update")
	}
	if updatedCJE.CondorRawEventID != cje.CondorRawEventID {
		t.Errorf("CondorRawEventIDs don't match after update")
	}
	if updatedCJE.DateTriggered.Format(time.RFC822Z) != cje.DateTriggered.Format(time.RFC822Z) {
		t.Errorf("DateTriggereds don't match after update")
	}
	err = d.DeleteCondorJobEvent(cje.ID)
	if err != nil {
		t.Error(err)
	}
}

func TestCRUDLastCondorJobEvent(t *testing.T) {
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
	jobID, err := d.InsertJob(jr)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if jobID == "" {
		t.Errorf("InsertJob returned an empty UUID.")
		t.Fail()
	}
	jr.ID = jobID
	ce := &CondorEvent{
		EventNumber: 9001,
		EventName:   "test_event",
		EventDesc:   "event for unit tests",
	}
	eventID, err := d.InsertCondorEvent(ce)
	if err != nil {
		t.Error(err)
	}
	ce.ID = eventID
	cr := &CondorRawEvent{
		JobID:         jr.ID,
		EventText:     "this is a unit test event",
		DateTriggered: time.Now(),
	}
	rawEventID, err := d.InsertCondorRawEvent(cr)
	if err != nil {
		t.Error(err)
	}
	cr.ID = rawEventID
	cje := &CondorJobEvent{
		JobID:            jr.ID,
		CondorEventID:    ce.ID,
		CondorRawEventID: cr.ID,
		DateTriggered:    time.Now(),
	}
	jobEventUUID, err := d.InsertCondorJobEvent(cje)
	if err != nil {
		t.Error(err)
	}
	cje.ID = jobEventUUID

	lj := &LastCondorJobEvent{
		JobID:            jr.ID,
		CondorJobEventID: cje.ID,
	}
	_, err = d.InsertLastCondorJobEvent(lj)
	if err != nil {
		t.Error(err)
	}
	retLJ, err := d.GetLastCondorJobEvent(lj.JobID)
	if err != nil {
		t.Error(err)
	}
	if retLJ.JobID != lj.JobID {
		t.Errorf("JobIDs don't match")
	}
	if retLJ.CondorJobEventID != lj.CondorJobEventID {
		t.Errorf("CondorJobEventIDs don't match")
	}
	newcje := &CondorJobEvent{
		JobID:            jr.ID,
		CondorEventID:    ce.ID,
		CondorRawEventID: cr.ID,
		DateTriggered:    time.Now(),
	}
	newJobEventUUID, err := d.InsertCondorJobEvent(newcje)
	if err != nil {
		t.Error(err)
	}
	newcje.ID = newJobEventUUID
	lj.CondorJobEventID = newcje.ID
	updatedLJ, err := d.UpdateLastCondorJobEvent(lj)
	if updatedLJ.JobID != lj.JobID {
		t.Errorf("JobIDs don't match after update")
	}
	if updatedLJ.CondorJobEventID != lj.CondorJobEventID {
		t.Errorf("CondorJobEventIDs don't match")
	}
	err = d.DeleteLastCondorJobEvent(lj.JobID)
	if err != nil {
		t.Error(err)
	}
}

func TestCondorJobStopRequest(t *testing.T) {
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
	jr.ID = newUUID

	sr := &CondorJobStopRequest{
		JobID:         jr.ID,
		Username:      "unit_tests",
		DateRequested: time.Now(),
		Reason:        "unit tests",
	}
	srID, err := d.InsertCondorJobStopRequest(sr)
	if err != nil {
		t.Error(err)
	}
	sr.ID = srID
	err = d.DeleteCondorJobStopRequest(sr.ID)
	if err != nil {
		t.Error(err)
	}
}
