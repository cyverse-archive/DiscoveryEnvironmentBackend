package events

import (
	"errors"
	"model"
	"os"
	"testing"
	"time"

	"github.com/pborman/uuid"
)

func ConnString() (string, error) {
	connString := os.Getenv("JEXDB")
	if connString == "" {
		return connString, errors.New("empty JEXDB string")
	}
	return connString, nil
}

func TestNewDatabaser(t *testing.T) {
	connString, err := ConnString()
	if err != nil {
		return
	}
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
	connString, err := ConnString()
	if err != nil {
		return
	}
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	submitted := time.Now()
	started := time.Now()
	completed := time.Now()
	condorID := "999"
	invID := uuid.New()
	jr := &model.JobRecord{
		BatchID:       "",
		Submitter:     "unit_tests",
		CondorID:      condorID,
		DateSubmitted: submitted,
		DateStarted:   started,
		DateCompleted: completed,
		AppID:         uuid.New(),
		InvocationID:  invID,
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
	if newJR.CondorID != jr.CondorID {
		t.Errorf("CondorIDs didn't match")
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
	updated.DateStarted = time.Now()
	upserted, err := d.UpsertJob(updated)
	if err != nil {
		t.Error(err)
	}
	if upserted.ID != updated.ID {
		t.Errorf("The IDs didn't match after an upsert")
	}
	if upserted.DateStarted == updated.DateStarted {
		t.Errorf("The DateStarteds didn't match after an upsert")
	}
	if updated.DateCompleted.Format(time.RFC822Z) != newJR.DateCompleted.Format(time.RFC822Z) {
		t.Errorf("Updated date completed fields don't match")
	}
	condor, err := d.GetJobByCondorID(condorID)
	if err != nil {
		t.Errorf("Error in GetJobByCondorID: %s", err)
	}
	if condor.ID != newUUID {
		t.Errorf("The IDs didn't match after GetJobByCondorID: %s %s", condor.ID, newUUID)
	}
	invocation, err := d.GetJobByInvocationID(invID)
	if err != nil {
		t.Errorf("Error in GetJobByInvocationID: %s", err)
	}
	if invocation.ID != newUUID {
		t.Errorf("The IDs didn't match after GetJobByInvocationID: %s %s", invocation.ID, newUUID)
	}
	err = d.DeleteJob(newUUID)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
}

// TestStringifyUUID tests the FixAppID function.
func TestStringifyUUID(t *testing.T) {
	jr := &model.JobRecord{}
	jr.AppID = stringifyUUID(nil)
	if jr.AppID != "" {
		t.Errorf("AppID was not an empty string after call to FixAppID: %s", jr.AppID)
	}

	jr.AppID = stringifyUUID([]uint8("000000"))
	if jr.AppID != "000000" {
		t.Errorf("AppID was not set to '000000' after call to FixAppID: %s", jr.AppID)
	}
}

func TestCRUDCondorEvents(t *testing.T) {
	connString, err := ConnString()
	if err != nil {
		return
	}
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	ce := &model.CondorEvent{
		EventNumber: "999",
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
	ce.EventNumber = "998"
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
	number, err := d.GetCondorEventByNumber(ce.EventNumber)
	if err != nil {
		t.Errorf("Error from GetCondorEventByNumber: %s", err)
	}
	if number.ID != ce.ID {
		t.Errorf("IDs don't match after GetCondorEventByNumber")
	}
	err = d.DeleteCondorEvent(updated.ID)
	if err != nil {
		t.Error(err)
	}
}

func TestCRUDCondorRawEvents(t *testing.T) {
	connString, err := ConnString()
	if err != nil {
		return
	}
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	submitted := time.Now()
	started := time.Now()
	completed := time.Now()
	jr := &model.JobRecord{
		BatchID:       "",
		Submitter:     "unit_tests",
		DateSubmitted: submitted,
		DateStarted:   started,
		DateCompleted: completed,
		AppID:         uuid.New(),
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

	ce := &model.CondorRawEvent{
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
	err = d.DeleteJob(jr.ID)
	if err != nil {
		t.Error(err)
	}
}

func TestCRUDCondorJobEvent(t *testing.T) {
	connString, err := ConnString()
	if err != nil {
		return
	}
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	submitted := time.Now()
	started := time.Now()
	completed := time.Now()
	jr := &model.JobRecord{
		BatchID:       "",
		Submitter:     "unit_tests",
		DateSubmitted: submitted,
		DateStarted:   started,
		DateCompleted: completed,
		AppID:         uuid.New(),
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
	ce := &model.CondorEvent{
		EventNumber: "001",
		EventName:   "test_event",
		EventDesc:   "event for unit tests",
	}
	eventID, err := d.InsertCondorEvent(ce)
	if err != nil {
		t.Error(err)
	}
	ce.ID = eventID
	cr := &model.CondorRawEvent{
		JobID:         jr.ID,
		EventText:     "this is a unit test event",
		DateTriggered: time.Now(),
	}
	rawEventID, err := d.InsertCondorRawEvent(cr)
	if err != nil {
		t.Error(err)
	}
	cr.ID = rawEventID
	cje := &model.CondorJobEvent{
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
	err = d.DeleteCondorRawEvent(cr.ID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteCondorEvent(ce.ID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteJob(jr.ID)
	if err != nil {
		t.Error(err)
	}
}

func TestCRUDLastCondorJobEvent(t *testing.T) {
	connString, err := ConnString()
	if err != nil {
		return
	}
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	submitted := time.Now()
	started := time.Now()
	completed := time.Now()
	jr := &model.JobRecord{
		BatchID:       "",
		Submitter:     "unit_tests",
		DateSubmitted: submitted,
		DateStarted:   started,
		DateCompleted: completed,
		AppID:         uuid.New(),
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
	ce := &model.CondorEvent{
		EventNumber: "001",
		EventName:   "test_event",
		EventDesc:   "event for unit tests",
	}
	eventID, err := d.InsertCondorEvent(ce)
	if err != nil {
		t.Error(err)
	}
	ce.ID = eventID
	cr := &model.CondorRawEvent{
		JobID:         jr.ID,
		EventText:     "this is a unit test event",
		DateTriggered: time.Now(),
	}
	rawEventID, err := d.InsertCondorRawEvent(cr)
	if err != nil {
		t.Error(err)
	}
	cr.ID = rawEventID
	cje := &model.CondorJobEvent{
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

	lj := &model.LastCondorJobEvent{
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
	newcje := &model.CondorJobEvent{
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
	err = d.DeleteCondorJobEvent(newcje.ID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteCondorRawEvent(cr.ID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteCondorEvent(ce.ID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteJob(jr.ID)
	if err != nil {
		t.Error(err)
	}
}

func TestCondorJobStopRequest(t *testing.T) {
	connString, err := ConnString()
	if err != nil {
		return
	}
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	submitted := time.Now()
	started := time.Now()
	completed := time.Now()
	jr := &model.JobRecord{
		BatchID:       "",
		Submitter:     "unit_tests",
		DateSubmitted: submitted,
		DateStarted:   started,
		DateCompleted: completed,
		AppID:         uuid.New(),
	}
	newUUID, err := d.InsertJob(jr)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	jr.ID = newUUID

	sr := &model.CondorJobStopRequest{
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
	newSR, err := d.GetCondorJobStopRequest(sr.ID)
	if err != nil {
		t.Error(err)
	}
	if newSR.ID != sr.ID {
		t.Errorf("IDs don't match")
	}
	if newSR.JobID != sr.JobID {
		t.Errorf("JobIDs don't match")
	}
	if newSR.Username != sr.Username {
		t.Errorf("Usernames don't match")
	}
	if newSR.DateRequested.Format(time.RFC822Z) != sr.DateRequested.Format(time.RFC822Z) {
		t.Errorf("DateRequesteds don't match")
	}
	if newSR.Reason != sr.Reason {
		t.Errorf("Reasons don't match")
	}
	sr.Reason = "poop"
	updated, err := d.UpdateCondorJobStopRequest(sr)
	if err != nil {
		t.Error(err)
	}
	if updated.ID != sr.ID {
		t.Errorf("IDs don't match after update")
	}
	if updated.JobID != sr.JobID {
		t.Errorf("JobIDs don't match after update")
	}
	if updated.Username != sr.Username {
		t.Errorf("Usernames don't match after update")
	}
	if updated.DateRequested.Format(time.RFC822Z) != sr.DateRequested.Format(time.RFC822Z) {
		t.Errorf("DateRequesteds don't match after update")
	}
	if updated.Reason != sr.Reason {
		t.Errorf("Reasons don't match after update")
	}
	err = d.DeleteCondorJobStopRequest(sr.ID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteJob(jr.ID)
	if err != nil {
		t.Error(err)
	}
}

func TestCRUDJobDeps(t *testing.T) {
	connString, err := ConnString()
	if err != nil {
		return
	}
	d, err := NewDatabaser(connString)
	if err != nil {
		t.Error(err)
	}
	defer d.db.Close()
	submitted := time.Now()
	started := time.Now()
	completed := time.Now()
	jr1 := &model.JobRecord{
		BatchID:       "",
		Submitter:     "unit_tests",
		DateSubmitted: submitted,
		DateStarted:   started,
		DateCompleted: completed,
		AppID:         uuid.New(),
	}
	jobID, err := d.InsertJob(jr1)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if jobID == "" {
		t.Errorf("InsertJob returned an empty UUID.")
		t.Fail()
	}
	jr1.ID = jobID
	jr2 := &model.JobRecord{
		BatchID:       "",
		Submitter:     "unit_tests",
		DateSubmitted: submitted,
		DateStarted:   started,
		DateCompleted: completed,
		AppID:         uuid.New(),
	}
	job2ID, err := d.InsertJob(jr2)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if job2ID == "" {
		t.Errorf("InsertJob returned an empty UUID.")
		t.Fail()
	}
	jr2.ID = job2ID
	jr3 := &model.JobRecord{
		BatchID:       "",
		Submitter:     "unit_tests",
		DateSubmitted: submitted,
		DateStarted:   started,
		DateCompleted: completed,
		AppID:         uuid.New(),
	}
	job3ID, err := d.InsertJob(jr3)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if job3ID == "" {
		t.Errorf("InsertJob returned an empty UUID.")
		t.Fail()
	}
	jr3.ID = job3ID
	dep1 := &model.CondorJobDep{
		PredecessorID: jr1.ID,
		SuccessorID:   jr2.ID,
	}
	dep2 := &model.CondorJobDep{
		PredecessorID: jr1.ID,
		SuccessorID:   jr3.ID,
	}
	err = d.InsertCondorJobDep(dep1)
	if err != nil {
		t.Error(err)
	}
	err = d.InsertCondorJobDep(dep2)
	if err != nil {
		t.Error(err)
	}
	preds, err := d.GetPredecessors(jr3.ID)
	if err != nil {
		t.Error(err)
	}
	if len(preds) != 1 {
		t.Errorf("Number of predecessors returned wasn't 1: %d", len(preds))
	}
	if preds[0].ID != jr1.ID {
		t.Errorf("ID of the predecessor was %s and should have been %s", preds[0].ID, jr1.ID)
	}
	succs, err := d.GetSuccessors(jr1.ID)
	if err != nil {
		t.Error(err)
	}
	if len(succs) != 2 {
		t.Errorf("Number of successors returned wasn't 2: %d", len(succs))
	}
	found2 := false
	found3 := false
	for _, s := range succs {
		if s.ID == jr2.ID {
			found2 = true
		}
		if s.ID == jr3.ID {
			found3 = true
		}
	}
	if !found2 {
		t.Errorf("Neither successor had an ID of %s", jr2.ID)
	}
	if !found3 {
		t.Errorf("Neither successor had an ID of %s", jr3.ID)
	}
	err = d.DeleteCondorJobDep(dep1.PredecessorID, dep1.SuccessorID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteCondorJobDep(dep2.PredecessorID, dep2.SuccessorID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteJob(jr1.ID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteJob(jr2.ID)
	if err != nil {
		t.Error(err)
	}
	err = d.DeleteJob(jr3.ID)
	if err != nil {
		t.Error(err)
	}
}
