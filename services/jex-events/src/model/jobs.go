package model

import "time"

// JobRecord is a type that contains info that goes into the jobs table.
type JobRecord struct {
	ID               string
	BatchID          string
	CondorID         string
	Submitter        string
	DateSubmitted    time.Time
	DateStarted      time.Time
	DateCompleted    time.Time
	AppID            string
	InvocationID     string
	ExitCode         int
	FailureThreshold int64
	FailureCount     int64
}

// CondorJobEvent ties a model.CondorEvent to a job and raw event.
type CondorJobEvent struct {
	ID               string
	JobID            string
	CondorEventID    string
	CondorRawEventID string
	Hash             string
	DateTriggered    time.Time
}

// CondorEvent contains info about an event that Condor emitted.
type CondorEvent struct {
	ID          string
	EventNumber string
	EventName   string
	EventDesc   string
}

// CondorRawEvent contains the raw, unparsed event that was emitted from Condor.
type CondorRawEvent struct {
	ID            string
	JobID         string
	EventText     string
	DateTriggered time.Time
}

// CondorJobDep tracks dependencies between jobs.
type CondorJobDep struct {
	SuccessorID   string
	PredecessorID string
}

// CondorJobStopRequest records a request to stop a job.
type CondorJobStopRequest struct {
	ID            string
	JobID         string
	Username      string
	DateRequested time.Time
	Reason        string
}

// LastCondorJobEvent records the last updated CondorJobEvent for a job.
type LastCondorJobEvent struct {
	JobID            string
	CondorJobEventID string
}

// Version contains info about the version of the database in use.
type Version struct {
	ID      int64
	Version string
	Applied time.Time
}
