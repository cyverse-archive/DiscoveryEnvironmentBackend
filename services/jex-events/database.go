package main

import (
	"database/sql"
	"time"

	_ "github.com/lib/pq"
)

// Databaser is a type used to interact with the database.
type Databaser struct {
	db         *sql.DB
	ConnString string
}

// NewDatabaser returns a pointer to a Databaser instnace that has already
// connected to the database by calling Ping().
func NewDatabaser(connString string) (*Databaser, error) {
	db, err := sql.Open("postgres", connString)
	if err != nil {
		return nil, err
	}
	err = db.Ping()
	if err != nil {
		return nil, err
	}
	databaser := &Databaser{
		db:         db,
		ConnString: connString,
	}
	return databaser, nil
}

// JobRecord is a type that contains info that goes into the jobs table.
type JobRecord struct {
	ID               string
	BatchID          string
	Submitter        string
	DateSubmitted    time.Time
	DateStarted      time.Time
	DateCompleted    time.Time
	AppID            string
	CommandLine      string
	EnvVariables     string
	ExitCode         int
	FailureThreshold int64
	FailureCount     int64
}

// InsertJob adds a new JobRecord to the database.
func (d *Databaser) InsertJob(jr *JobRecord) (string, error) {
	query := `
	INSERT INTO jobs (
		batch_id,
		submitter,
		date_submitted,
		date_started,
		date_completed,
		app_id,
		command_line,
		env_variables,
		exit_code,
		failure_threshold,
		failure_count
	) VALUES (
		cast($1 AS uuid),
		$2,
		$3,
		$4,
		$5,
		cast($6 as uuid),
		$7,
		$8,
		$9,
		$10,
		$11
	) RETURNING id`
	var fixedBatch *string
	if jr.BatchID == "" {
		fixedBatch = nil
	} else {
		fixedBatch = &jr.BatchID
	}
	var id string
	err := d.db.QueryRow(
		query,
		fixedBatch,
		jr.Submitter,
		jr.DateSubmitted,
		jr.DateStarted,
		jr.DateCompleted,
		jr.AppID,
		jr.CommandLine,
		jr.EnvVariables,
		jr.ExitCode,
		jr.FailureThreshold,
		jr.FailureCount,
	).Scan(&id)
	if err != nil {
		return "", err
	}
	return id, err
}

// DeleteJob removes a JobRecord from the database.
func (d *Databaser) DeleteJob(uuid string) error {
	query := `DELETE FROM jobs WHERE id = cast($1 as uuid)`
	_, err := d.db.Exec(query, uuid)
	if err != nil {
		return err
	}
	return nil
}

// GetJob returns a JobRecord from the database.
func (d *Databaser) GetJob(uuid string) (*JobRecord, error) {
	query := `
	SELECT cast(id as varchar),
				batch_id,
				submitter,
				date_submitted,
				date_started,
				date_completed,
				cast(app_id as varchar),
				command_line,
				env_variables,
				exit_code,
				failure_threshold,
				failure_count
		FROM jobs
	WHERE id = cast($1 as uuid)
	`
	rows := d.db.QueryRow(query, uuid)
	var id string
	var batchid interface{}
	var submitter string
	var datesubmitted time.Time
	var datestarted time.Time
	var datecompleted time.Time
	var appid string
	var commandline string
	var envvariables string
	var exitcode int
	var failurethreshold int64
	var failurecount int64
	err := rows.Scan(
		&id,
		&batchid,
		&submitter,
		&datesubmitted,
		&datestarted,
		&datecompleted,
		&appid,
		&commandline,
		&envvariables,
		&exitcode,
		&failurethreshold,
		&failurecount,
	)
	jr := JobRecord{
		ID:               id,
		Submitter:        submitter,
		DateSubmitted:    datesubmitted,
		DateStarted:      datestarted,
		DateCompleted:    datecompleted,
		AppID:            appid,
		CommandLine:      commandline,
		EnvVariables:     envvariables,
		ExitCode:         exitcode,
		FailureThreshold: failurethreshold,
		FailureCount:     failurecount,
	}
	if batchid == nil {
		jr.BatchID = ""
	} else {
		jr.BatchID = batchid.(string)
	}
	return &jr, err
}

// UpdateJob updates a job instance in the database
func (d *Databaser) UpdateJob(jr *JobRecord) (*JobRecord, error) {
	query := `
	UPDATE jobs
		SET batch_id = cast($1 as uuid),
				submitter = $2,
				date_submitted = $3,
				date_started = $4,
				date_completed = $5,
				app_id = cast($6 as uuid),
				command_line = $7,
				env_variables = $8,
				exit_code = $9,
				failure_threshold = $10,
				failure_count = $11
	WHERE id = cast($12 as uuid)
	RETURNING id
	`
	var id string
	var batchid *string
	if jr.BatchID == "" {
		batchid = nil
	} else {
		batchid = &jr.BatchID
	}
	err := d.db.QueryRow(
		query,
		batchid,
		jr.Submitter,
		jr.DateSubmitted,
		jr.DateStarted,
		jr.DateCompleted,
		jr.AppID,
		jr.CommandLine,
		jr.EnvVariables,
		jr.ExitCode,
		jr.FailureThreshold,
		jr.FailureCount,
		jr.ID,
	).Scan(&id)
	if err != nil {
		return nil, err
	}
	updated, err := d.GetJob(id)
	if err != nil {
		return updated, err
	}
	return updated, nil
}

// CondorEvent contains info about an event that Condor emitted.
type CondorEvent struct {
	ID          string
	EventNumber int
	EventName   string
	EventDesc   string
}

// InsertCondorEvent adds a new CondorEvent to the database. The ID field is
// ignored.
func (d *Databaser) InsertCondorEvent(ce *CondorEvent) (string, error) {
	query := `
	INSERT INTO condor_events (
		event_number,
		event_name,
		event_desc
		) VALUES (
			$1,
			$2,
			$3
		) RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		ce.EventNumber,
		ce.EventName,
		ce.EventDesc,
	).Scan(&id)
	if err != nil {
		return "", err
	}
	return id, nil
}

// DeleteCondorEvent removes a CondorEvent from the database by its uuid.
func (d *Databaser) DeleteCondorEvent(uuid string) error {
	query := `
	DELETE FROM condor_events WHERE id = cast($1 as uuid)
	`
	_, err := d.db.Exec(query, uuid)
	if err != nil {
		return err
	}
	return nil
}

// GetCondorEvent gets a CondorEvent from the database and returns a pointer to
// a filled out instance of CondorEvent.
func (d *Databaser) GetCondorEvent(uuid string) (*CondorEvent, error) {
	query := `
	SELECT id,
				 event_number,
				 event_name,
				 event_desc
	  FROM condor_events
	 WHERE id = cast($1 as uuid)
	`
	var id string
	var eventNumber int
	var eventName string
	var eventDesc string
	err := d.db.QueryRow(
		query,
		uuid,
	).Scan(
		&id,
		&eventNumber,
		&eventName,
		&eventDesc,
	)
	if err != nil {
		return nil, err
	}
	ce := &CondorEvent{
		ID:          id,
		EventNumber: eventNumber,
		EventName:   eventName,
		EventDesc:   eventDesc,
	}
	return ce, nil
}

// UpdateCondorEvent updates a CondorEvent in the database. The CondorEvent must
// be fully filled out with information, not just the fields that you want to
// update.
func (d *Databaser) UpdateCondorEvent(ce *CondorEvent) (*CondorEvent, error) {
	query := `
	UPDATE condor_events
	   SET event_number = $1,
		     event_name = $2,
				 event_desc = $3
	 WHERE id = cast($4 as uuid)
	RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		ce.EventNumber,
		ce.EventName,
		ce.EventDesc,
		ce.ID,
	).Scan(&id)
	if err != nil {
		return nil, err
	}
	updated, err := d.GetCondorEvent(id)
	if err != nil {
		return nil, err
	}
	return updated, nil

}

// CondorRawEvent contains the raw, unparsed event that was emitted from Condor.
type CondorRawEvent struct {
	ID            string
	JobID         string
	EventText     string
	DateTriggered time.Time
}

// InsertCondorRawEvent adds an unparsed event record to the database.
func (d *Databaser) InsertCondorRawEvent(re *CondorRawEvent) (string, error) {
	query := `
	INSERT INTO condor_raw_events (
		job_id,
		event_text,
		date_triggered
		) VALUES (
			cast($1 as uuid),
			$2,
			$3
		) RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		re.JobID,
		re.EventText,
		re.DateTriggered,
	).Scan(&id)
	if err != nil {
		return "", err
	}
	return id, nil
}

// DeleteCondorRawEvent removes an unparsed job event from the database.
func (d *Databaser) DeleteCondorRawEvent(uuid string) error {
	query := `
	DELETE FROM condor_raw_events WHERE id = cast($1 as uuid)
	`
	_, err := d.db.Exec(query, uuid)
	if err != nil {
		return err
	}
	return nil
}

// GetCondorRawEvent retrieves an unparsed job event from the database.
func (d *Databaser) GetCondorRawEvent(uuid string) (*CondorRawEvent, error) {
	query := `
	SELECT id,
	       job_id,
				 event_text,
				 date_triggered
	  FROM condor_raw_events
	 WHERE id = cast($1 as uuid)
	`
	var id string
	var jobID string
	var eventText string
	var dateTriggered time.Time
	err := d.db.QueryRow(query, uuid).Scan(
		&id,
		&jobID,
		&eventText,
		&dateTriggered,
	)
	if err != nil {
		return nil, err
	}
	re := &CondorRawEvent{
		ID:            id,
		JobID:         jobID,
		EventText:     eventText,
		DateTriggered: dateTriggered,
	}
	return re, nil
}

// UpdateCondorRawEvent updates a record of an unparsed job event.
func (d *Databaser) UpdateCondorRawEvent(re *CondorRawEvent) (*CondorRawEvent, error) {
	query := `
	UPDATE condor_raw_events
	   SET job_id = cast($1 as uuid),
		     event_text = $2,
				 date_triggered = $3
	 WHERE id = cast($4 as uuid)
	RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		re.JobID,
		re.EventText,
		re.DateTriggered,
		re.ID,
	).Scan(&id)
	if err != nil {
		return nil, err
	}
	updated, err := d.GetCondorRawEvent(id)
	if err != nil {
		return nil, err
	}
	return updated, nil
}

// CondorJobEvent ties a CondorEvent to a job and raw event.
type CondorJobEvent struct {
	ID               string
	JobID            string
	CondorEventID    string
	CondorRawEventID string
	DateTriggered    time.Time
}

// InsertCondorJobEvent adds a parsed job event to the database.
func (d *Databaser) InsertCondorJobEvent(je *CondorJobEvent) (string, error) {
	query := `
	INSERT INO condor_job_events (
		job_id,
		condor_event_id,
		condor_raw_event_id,
		date_triggered
		) VALUES (
			cast($1 as uuid),
			cast($2 as uuid),
			cast($3 as uuid),
			$4
	  ) RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		je.JobID,
		je.CondorEventID,
		je.CondorRawEventID,
		je.DateTriggered,
	).Scan(&id)
	if err != nil {
		return "", err
	}
	return id, nil
}

// DeleteCondorJobEvent removes a parsed job event from the database.
func (d *Databaser) DeleteCondorJobEvent(uuid string) error {
	query := `
	DELETE FROM condor_job_events WHERE id = cast($1 as uuid)
	`
	_, err := d.db.Exec(query, uuid)
	if err != nil {
		return err
	}
	return nil
}

// GetCondorJobEvent returns a pointer to an instance of CondorJobEvent that's
// been filled in with data from the database.
func (d *Databaser) GetCondorJobEvent(uuid string) (*CondorJobEvent, error) {
	query := `
	SELECT id,
	       job_id,
				 condor_event_id,
				 condor_raw_event_id,
				 date_triggered
	  FROM condor_job_events
	 WHERE id = cast($1 as uuid)
	`
	var id string
	var jobID string
	var condorEventID string
	var condorRawEventID string
	var dateTriggered time.Time
	err := d.db.QueryRow(query, uuid).Scan(
		&id,
		&jobID,
		&condorEventID,
		&condorRawEventID,
		&dateTriggered,
	)
	if err != nil {
		return nil, err
	}
	je := &CondorJobEvent{
		ID:               id,
		JobID:            jobID,
		CondorEventID:    condorEventID,
		CondorRawEventID: condorRawEventID,
		DateTriggered:    dateTriggered,
	}
	return je, nil
}

// UpdateCondorJobEvent updates values for a parsed job event in the database.
// The CondorJobEvent that gets passed in must have all fields set ot the
// desired values.
func (d *Databaser) UpdateCondorJobEvent(je *CondorJobEvent) (*CondorJobEvent, error) {
	query := `
	UPDATE condor_job_events
	   SET job_id = cast($1 as uuid),
		     condor_event_id = cast($2 as uuid),
				 condor_raw_event_id = cast($3 as uuid),
				 date_triggered = $4
	 WHERE id = cast($1 as uuid)
	RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		je.JobID,
		je.CondorEventID,
		je.CondorRawEventID,
		je.DateTriggered,
	).Scan(&id)
	if err != nil {
		return nil, err
	}
	updated, err := d.GetCondorJobEvent(id)
	if err != nil {
		return nil, err
	}
	return updated, nil
}

// LastCondorJobEvent records the last updated CondorJobEvent for a job.
type LastCondorJobEvent struct {
	JobID            string
	CondorJobEventID string
}

// InsertLastCondorJobEvent adds an entry that points to the last event for a
// job.
func (d *Databaser) InsertLastCondorJobEvent(je *LastCondorJobEvent) (string, error) {
	query := `
	INSERT INTO last_condor_job_events (
		job_id,
		condor_job_event_id
	) VALUES (
		cast($1 as uuid),
		cast($2 as uuid)
	) RETURNING job_id
	`
	var jobID string
	err := d.db.QueryRow(
		query,
		je.JobID,
		je.CondorJobEventID,
	).Scan(&jobID)
	if err != nil {
		return "", err
	}
	return jobID, nil
}

// DeleteLastCondorJobEvent removes the entry for job that points to the last
// event.
func (d *Databaser) DeleteLastCondorJobEvent(uuid string) error {
	query := `
	DELETE FROM last_condor_job_events WHERE job_id = cast($1 as uuid)
	`
	_, err := d.db.Exec(query, uuid)
	if err != nil {
		return err
	}
	return nil
}

// GetLastCondorJobEvent returns a record that tells what the last event for a
// job was.
func (d *Databaser) GetLastCondorJobEvent(uuid string) (*LastCondorJobEvent, error) {
	query := `
	SELECT job_id,
	       condor_job_event_id
		FROM last_condor_job_events
	 WHERE job_id = cast($1 as uuid)
	`
	var jobID string
	var condorJobEventID string
	err := d.db.QueryRow(query, uuid).Scan(
		&jobID,
		&condorJobEventID,
	)
	if err != nil {
		return nil, err
	}
	je := &LastCondorJobEvent{
		JobID:            jobID,
		CondorJobEventID: condorJobEventID,
	}
	return je, nil
}

// UpdateLastCondorJobEvent modifies the record that tells what the last event
// for a job was.
func (d *Databaser) UpdateLastCondorJobEvent(je *LastCondorJobEvent) (*LastCondorJobEvent, error) {
	query := `
	UPDATE last_condor_job_events
	   SET condor_job_event_id = cast($1 as uuid)
	 WHERE job_id = cast($2 as uuid)
	RETURNING job_id
	`
	var jobID string
	err := d.db.QueryRow(query, je.CondorJobEventID, je.JobID).Scan(&jobID)
	if err != nil {
		return nil, err
	}
	updated, err := d.GetLastCondorJobEvent(jobID)
	if err != nil {
		return nil, err
	}
	return updated, nil
}

// CondorJobStopRequest records a request to stop a job.
type CondorJobStopRequest struct {
	ID            string
	JobID         string
	Username      string
	DateRequested time.Time
	Reason        string
}

// InsertCondorJobStopRequest adds a record of a job stop request.
func (d *Databaser) InsertCondorJobStopRequest(jr *CondorJobStopRequest) (string, error) {
	query := `
	INSERT INTO condor_job_stop_requests (
		job_id,
		username,
		date_requested,
		reason
	) VALUES (
			cast($1 as uuid),
			$2,
			$3,
			$5
	) RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		jr.JobID,
		jr.Username,
		jr.DateRequested,
		jr.Reason,
	).Scan(&id)
	if err != nil {
		return "", err
	}
	return id, nil
}

// DeleteCondorJobStopRequest deletes the record of a job stop request.
func (d *Databaser) DeleteCondorJobStopRequest(uuid string) error {
	query := `
	DELETE FROM codnor_job_stop_requests WHERE id = cast($1 as uuid)
	`
	_, err := d.db.Exec(query, uuid)
	if err != nil {
		return err
	}
	return nil
}

// GetCondorJobStopRequest returns the record of a job stop request.
func (d *Databaser) GetCondorJobStopRequest(uuid string) (*CondorJobStopRequest, error) {
	query := `
	SELECT id,
	       job_id,
				 username,
				 date_requested,
				 reason
	  FROM condor_job_stop_requests
	 WHERE id = cast($1 as uuid)
	`
	var id string
	var jobID string
	var username string
	var dateRequested time.Time
	var reason string
	err := d.db.QueryRow(
		query,
		uuid,
	).Scan(
		&id,
		&jobID,
		&username,
		&dateRequested,
		&reason,
	)
	if err != nil {
		return nil, err
	}
	jr := &CondorJobStopRequest{
		ID:            id,
		JobID:         jobID,
		Username:      username,
		DateRequested: dateRequested,
		Reason:        reason,
	}
	return jr, nil
}

// UpdateCondorJobStopRequest updates the record of a job stop request.
func (d *Databaser) UpdateCondorJobStopRequest(jr *CondorJobStopRequest) (*CondorJobStopRequest, error) {
	query := `
	UPDATE condor_job_stop_requests
	   SET job_id = cast($1 as uuid),
		     username = $2,
				 date_requested = $3,
				 reason = $4
	 WHERE id = cast($5 as uuid)
	RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		jr.JobID,
		jr.Username,
		jr.DateRequested,
		jr.Reason,
	).Scan(&id)
	if err != nil {
		return nil, err
	}
	updated, err := d.GetCondorJobStopRequest(id)
	if err != nil {
		return nil, err
	}
	return updated, nil
}

// CondorJobDep tracks dependencies between jobs.
type CondorJobDep struct {
	SuccessorID   string
	PredecessorID string
}

// InsertCondorJobDep adds a job dependency to the database.
func (d *Databaser) InsertCondorJobDep(jd *CondorJobDep) (string, error) {
	query := `
	INSERT INTO condor_job_deps (
		successor_id,
		predecessor_id
	) VALUES (
		cast($1 as uuid),
		cast($2 as uuid)
	) RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		jd.SuccessorID,
		jd.PredecessorID,
	).Scan(&id)
	if err != nil {
		return "", err
	}
	return id, nil
}

// DeleteCondorJobDep removes a job dependency from the database.
func (d *Databaser) DeleteCondorJobDep(predUUID, succUUID string) error {
	query := `
	DELETE FROM condor_job_deps WHERE successor_id = cast($1 as uuid) AND predecessor_id = cast($2 as uuid)
	`
	_, err := d.db.Exec(query, succUUID, predUUID)
	if err != nil {
		return err
	}
	return nil
}

// Version contains info about the version of the database in use.
type Version struct {
	ID      int64
	Version string
	Applied time.Time
}
