package events

import (
	"database/sql"
	"fmt"
	"model"
	"strings"
	"time"

	"github.com/lib/pq"
)

var epoch = time.Unix(0, 0)

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

func nilify(val *string) *string {
	if *val == "" {
		return nil
	}
	return val
}

// stringifyUUID returns a string representation of a UUID, with the empty string
// being used to represent a null UUID.
func stringifyUUID(nullableUUID interface{}) string {
	if nullableUUID == nil {
		return ""
	}
	return string(nullableUUID.([]uint8))
}

// This evil has been perpetrated to avoid an issue where time.Time instances
// set to their zero value and stored in PostgreSQL with timezone info can
// come back as Time instances from __before__ the epoch. We need to re-zero
// the dates on the fly when that happens.
func epochizeTime(timestamp *time.Time) *time.Time {
	if timestamp.Before(epoch) {
		return &epoch
	}
	return timestamp
}

// InsertJob adds a new model.JobRecord to the database.
func (d *Databaser) InsertJob(jr *model.JobRecord) (string, error) {
	query := `
	INSERT INTO jobs (
		batch_id,
		submitter,
		date_submitted,
		date_started,
		date_completed,
		app_id,
		exit_code,
		failure_threshold,
		failure_count,
		condor_id,
		invocation_id
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
	var id string
	err := d.db.QueryRow(
		query,
		nilify(&jr.BatchID),
		jr.Submitter,
		jr.DateSubmitted,
		jr.DateStarted,
		jr.DateCompleted,
		nilify(&jr.AppID),
		jr.ExitCode,
		jr.FailureThreshold,
		jr.FailureCount,
		jr.CondorID,
		nilify(&jr.InvocationID),
	).Scan(&id)
	if err != nil {
		return "", err
	}
	return id, err
}

// AddJob add a new model.JobRecord to the database in a more friendly way than
// InsertJob. Only adds the job if it doesn't already exist.
// Uses InsertJob under the hood. Used for new jobs.
func (d *Databaser) AddJob(condorID string) (*model.JobRecord, error) {
	job, err := d.GetJobByCondorID(condorID)
	if err == sql.ErrNoRows {
		jr := &model.JobRecord{
			CondorID: condorID,
		}
		id, err := d.InsertJob(jr)
		if err != nil {
			logger.Printf("Error inserting job: %s", err)
			return nil, err
		}
		jr.ID = id
		return jr, nil
	}
	if err != nil {
		logger.Printf("Error getting job by condor id: %s", err)
		return nil, err
	}
	return job, nil
}

var _upsertJobColumns = []string{
	"batch_id",
	"submitter",
	"date_submitted",
	"date_started",
	"date_completed",
	"app_id",
	"exit_code",
	"failure_threshold",
	"failure_count",
	"condor_id",
	"invocation_id",
}

const _upsertJobStmt = `
    WITH new_values ( %[1]s ) AS (
        VALUES (
           cast($1 AS uuid),
           $2,
           cast($3 AS timestamp with time zone),
           cast($4 AS timestamp with time zone),
           cast($5 AS timestamp with time zone),
           cast($6 AS uuid),
           cast($7 AS integer),
           cast($8 AS integer),
           cast($9 AS integer),
           $10,
           cast($11 AS uuid )
        )
    ),
    upsert AS (
        UPDATE jobs j
        SET %[2]s
        FROM new_values nv
        WHERE j.condor_id = nv.condor_id
        RETURNING j.*
    )
    INSERT INTO jobs (%[1]s)
    SELECT %[1]s
    FROM new_values
    WHERE NOT EXISTS (
        SELECT 1
        FROM upsert up
        WHERE up.condor_id = new_values.condor_id
    )
`

const _jobByCondorIDQuery = `
    SELECT cast(id as varchar),
           batch_id,
           submitter,
           date_submitted,
           date_started,
           date_completed,
           app_id,
           exit_code,
           failure_threshold,
           failure_count,
           condor_id,
           invocation_id
    FROM jobs
    WHERE condor_id = $1 `

func upsertSetList(prefix string) string {
	columns := make([]string, len(_upsertJobColumns), len(_upsertJobColumns))
	for i := 0; i < len(_upsertJobColumns); i++ {
		column := _upsertJobColumns[i]
		columns[i] = fmt.Sprintf("%s = %s.%s", column, prefix, column)
	}
	return strings.Join(columns, ", ")
}

func upsertJobStmt() string {
	stdColumnList := strings.Join(_upsertJobColumns, ", ")
	return fmt.Sprintf(_upsertJobStmt, stdColumnList, upsertSetList("nv"))
}

// Attempts to perform a job upsert.
func attemptJobUpsert(stmt *sql.Stmt, jr *model.JobRecord) (sql.Result, error) {
	return stmt.Exec(
		nilify(&jr.BatchID),
		jr.Submitter,
		jr.DateSubmitted,
		jr.DateStarted,
		jr.DateCompleted,
		nilify(&jr.AppID),
		jr.ExitCode,
		jr.FailureThreshold,
		jr.FailureCount,
		jr.CondorID,
		nilify(&jr.InvocationID),
	)
}

// Retrieves an upserted job.
func getUpsertedJob(tx *sql.Tx, condorID string) (*model.JobRecord, error) {
	row := tx.QueryRow(_jobByCondorIDQuery, condorID)
	return jobRecordFromRow(row)
}

// UpsertJob updates a job if it already exists, otherwise it inserts a new job
// into the database.
func (d *Databaser) UpsertJob(jr *model.JobRecord) (*model.JobRecord, error) {
	var updatedJr *model.JobRecord

	// This needs to be done inside a transaction.
	tx, err := d.db.Begin()
	if err != nil {
		logger.Println("Unable to start a transaction:", err)
		return nil, err
	}

	// Prepare the SQL statement.
	stmt, err := tx.Prepare(upsertJobStmt())
	if err != nil {
		logger.Println("Unable to prepare job upsert statement:", err)
		tx.Rollback()
		return nil, err
	}

	// Attempt to insert or update the job.
	_, err = attemptJobUpsert(stmt, jr)
	if err != nil {
		pqErr, ok := err.(*pq.Error)
		if ok && pqErr.Code.Name() == "unique_violation" {
			logger.Println("Unique violation on job upsert - retrying")
		} else {
			logger.Println("Error upserting job:", err)
			tx.Rollback()
			return nil, err
		}
	} else {
		updatedJr, err = getUpsertedJob(tx, jr.CondorID)
		if err != nil {
			logger.Println("unable to look up job with Condor ID", jr.CondorID)
			return nil, err
		}
	}

	// Retry the upsert if necessary.
	if updatedJr == nil {
		_, err := attemptJobUpsert(stmt, jr)
		if err != nil {
			logger.Println("Error upserting job:", err)
			tx.Rollback()
			return nil, err
		}
		updatedJr, err = getUpsertedJob(tx, jr.CondorID)
		if err != nil {
			logger.Println("unable to look up job with Condor ID", jr.CondorID)
			return nil, err
		}
	}

	// Commit the transaction.
	err = tx.Commit()
	if err != nil {
		return nil, err
	}
	return updatedJr, nil
}

// DeleteJob removes a model.JobRecord from the database.
func (d *Databaser) DeleteJob(uuid string) error {
	query := `DELETE FROM jobs WHERE id = cast($1 as uuid)`
	_, err := d.db.Exec(query, uuid)
	if err != nil {
		return err
	}
	return nil
}

// jobRecordFromRow converts a row from a result set to a job record
func jobRecordFromRow(row *sql.Row) (*model.JobRecord, error) {
	jr := &model.JobRecord{}

	// Workaround for nullable UUID fields in the database.
	var batchid interface{}
	var appid interface{}
	var invid interface{}

	err := row.Scan(
		&jr.ID,
		&batchid,
		&jr.Submitter,
		&jr.DateSubmitted,
		&jr.DateStarted,
		&jr.DateCompleted,
		&appid,
		&jr.ExitCode,
		&jr.FailureThreshold,
		&jr.FailureCount,
		&jr.CondorID,
		&invid,
	)

	// Update the nullable UUID fields in the job record.
	jr.BatchID = stringifyUUID(batchid)
	jr.AppID = stringifyUUID(appid)
	jr.InvocationID = stringifyUUID(invid)

	// Fix any malformed timestamps.
	jr.DateSubmitted = *epochizeTime(&jr.DateSubmitted)
	jr.DateStarted = *epochizeTime(&jr.DateStarted)
	jr.DateCompleted = *epochizeTime(&jr.DateCompleted)

	return jr, err
}

// GetJob returns a model.JobRecord from the database.
func (d *Databaser) GetJob(uuid string) (*model.JobRecord, error) {
	query := `
	SELECT cast(id as varchar),
				 batch_id,
				 submitter,
				 date_submitted,
				 date_started,
				 date_completed,
				 app_id,
				 exit_code,
				 failure_threshold,
				 failure_count,
				 condor_id,
				 invocation_id
	  FROM jobs
	 WHERE id = cast($1 as uuid)
	`
	row := d.db.QueryRow(query, uuid)
	return jobRecordFromRow(row)
}

// GetJobByCondorID returns a model.JobRecord from the database.
func (d *Databaser) GetJobByCondorID(condorID string) (*model.JobRecord, error) {
	row := d.db.QueryRow(_jobByCondorIDQuery, condorID)
	return jobRecordFromRow(row)
}

// GetJobByInvocationID returns a model.JobRecord from the database.
func (d *Databaser) GetJobByInvocationID(invocationID string) (*model.JobRecord, error) {
	query := `
	SELECT cast(id as varchar),
	       batch_id,
				 submitter,
				 date_submitted,
				 date_started,
				 date_completed,
				 app_id,
				 exit_code,
				 failure_threshold,
				 failure_count,
				 condor_id,
				 invocation_id
	  FROM jobs
	 WHERE invocation_id = cast($1 as uuid)
	`
	row := d.db.QueryRow(query, invocationID)
	return jobRecordFromRow(row)
}

// UpdateJob updates a job instance in the database
func (d *Databaser) UpdateJob(jr *model.JobRecord) (*model.JobRecord, error) {
	query := `
	UPDATE jobs
		SET batch_id = cast($1 as uuid),
				submitter = $2,
				date_submitted = $3,
				date_started = $4,
				date_completed = $5,
				app_id = cast($6 as uuid),
				exit_code = $7,
				failure_threshold = $8,
				failure_count = $9,
				condor_id = $10,
				invocation_id = $11
	WHERE id = cast($12 as uuid)
	RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		nilify(&jr.BatchID),
		jr.Submitter,
		jr.DateSubmitted,
		jr.DateStarted,
		jr.DateCompleted,
		nilify(&jr.AppID),
		jr.ExitCode,
		jr.FailureThreshold,
		jr.FailureCount,
		jr.CondorID,
		nilify(&jr.InvocationID),
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

// InsertCondorEvent adds a new model.CondorEvent to the database. The ID field is
// ignored.
func (d *Databaser) InsertCondorEvent(ce *model.CondorEvent) (string, error) {
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

// DeleteCondorEvent removes a model.CondorEvent from the database by its uuid.
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

// GetCondorEvent gets a model.CondorEvent from the database and returns a pointer to
// a filled out instance of model.CondorEvent.
func (d *Databaser) GetCondorEvent(uuid string) (*model.CondorEvent, error) {
	query := `
	SELECT id,
				 event_number,
				 event_name,
				 event_desc
	  FROM condor_events
	 WHERE id = cast($1 as uuid)
	`
	var id string
	var eventNumber string
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
	ce := &model.CondorEvent{
		ID:          id,
		EventNumber: eventNumber,
		EventName:   eventName,
		EventDesc:   eventDesc,
	}
	return ce, nil
}

// GetCondorEventByNumber gets a model.CondorEvent from the database and returns a pointer to
// a filled out instance of model.CondorEvent.
func (d *Databaser) GetCondorEventByNumber(number string) (*model.CondorEvent, error) {
	query := `
	SELECT id,
				event_number,
				event_name,
				event_desc
		FROM condor_events
	WHERE event_number = $1
	`
	var id string
	var eventNumber string
	var eventName string
	var eventDesc string
	err := d.db.QueryRow(
		query,
		number,
	).Scan(
		&id,
		&eventNumber,
		&eventName,
		&eventDesc,
	)
	if err != nil {
		return nil, err
	}
	ce := &model.CondorEvent{
		ID:          id,
		EventNumber: eventNumber,
		EventName:   eventName,
		EventDesc:   eventDesc,
	}
	return ce, nil
}

// UpdateCondorEvent updates a model.CondorEvent in the database. The model.CondorEvent must
// be fully filled out with information, not just the fields that you want to
// update.
func (d *Databaser) UpdateCondorEvent(ce *model.CondorEvent) (*model.CondorEvent, error) {
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

// InsertCondorRawEvent adds an unparsed event record to the database.
func (d *Databaser) InsertCondorRawEvent(re *model.CondorRawEvent) (string, error) {
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

// AddCondorRawEvent adds a raw event to the database. You'll probably want to
// use this instead of InsertCondorRawEvent.
func (d *Databaser) AddCondorRawEvent(eventText string, jobID string) (string, error) {
	re := &model.CondorRawEvent{
		JobID:         jobID,
		EventText:     eventText,
		DateTriggered: time.Now(),
	}
	reID, err := d.InsertCondorRawEvent(re)
	if err != nil {
		return "", err
	}
	return reID, nil
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
func (d *Databaser) GetCondorRawEvent(uuid string) (*model.CondorRawEvent, error) {
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
	re := &model.CondorRawEvent{
		ID:            id,
		JobID:         jobID,
		EventText:     eventText,
		DateTriggered: dateTriggered,
	}
	return re, nil
}

// UpdateCondorRawEvent updates a record of an unparsed job event.
func (d *Databaser) UpdateCondorRawEvent(re *model.CondorRawEvent) (*model.CondorRawEvent, error) {
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

// InsertCondorJobEvent adds a parsed job event to the database.
func (d *Databaser) InsertCondorJobEvent(je *model.CondorJobEvent) (string, error) {
	query := `
	INSERT INTO condor_job_events (
		job_id,
		condor_event_id,
		condor_raw_event_id,
		date_triggered,
		checksum
	) VALUES (
	  cast($1 as uuid),
		cast($2 as uuid),
		cast($3 as uuid),
		$4,
		$5
	) RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		je.JobID,
		je.CondorEventID,
		je.CondorRawEventID,
		je.DateTriggered,
		je.Hash,
	).Scan(&id)
	if err != nil {
		return "", err
	}
	return id, nil
}

// DoesCondorJobEventExist returns true if an event with a matching checksum
// is already in the database.
func (d *Databaser) DoesCondorJobEventExist(checksum string) (bool, error) {
	query := `
	SELECT COUNT(*) as job_count FROM condor_job_events where checksum = $1
	`
	var count int64
	err := d.db.QueryRow(query, checksum).Scan(&count)
	if err != nil {
		return false, err
	}
	if count > 0 {
		return true, nil
	}
	return false, nil
}

// AddCondorJobEvent adds a model.CondorJobEvent to the database. You'll probably want
// to use this over InsertCondorJobEvent.
func (d *Databaser) AddCondorJobEvent(jobID string, eventID string, rawEventID string, hash string) (string, error) {
	je := &model.CondorJobEvent{
		JobID:            jobID,
		CondorEventID:    eventID,
		CondorRawEventID: rawEventID,
		DateTriggered:    time.Now(),
		Hash:             hash,
	}
	jeID, err := d.InsertCondorJobEvent(je)
	if err != nil {
		return "", err
	}
	return jeID, nil
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

// GetCondorJobEvent returns a pointer to an instance of model.CondorJobEvent that's
// been filled in with data from the database.
func (d *Databaser) GetCondorJobEvent(uuid string) (*model.CondorJobEvent, error) {
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
	je := &model.CondorJobEvent{
		ID:               id,
		JobID:            jobID,
		CondorEventID:    condorEventID,
		CondorRawEventID: condorRawEventID,
		DateTriggered:    dateTriggered,
	}
	return je, nil
}

// UpdateCondorJobEvent updates values for a parsed job event in the database.
// The model.CondorJobEvent that gets passed in must have all fields set ot the
// desired values.
func (d *Databaser) UpdateCondorJobEvent(je *model.CondorJobEvent) (*model.CondorJobEvent, error) {
	query := `
	UPDATE condor_job_events
	   SET job_id = cast($1 as uuid),
		     condor_event_id = cast($2 as uuid),
				 condor_raw_event_id = cast($3 as uuid),
				 date_triggered = $4
	 WHERE id = cast($5 as uuid)
	RETURNING id
	`
	var id string
	err := d.db.QueryRow(
		query,
		je.JobID,
		je.CondorEventID,
		je.CondorRawEventID,
		je.DateTriggered,
		je.ID,
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

// InsertLastCondorJobEvent adds an entry that points to the last event for a
// job.
func (d *Databaser) InsertLastCondorJobEvent(je *model.LastCondorJobEvent) (string, error) {
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
func (d *Databaser) GetLastCondorJobEvent(uuid string) (*model.LastCondorJobEvent, error) {
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
	je := &model.LastCondorJobEvent{
		JobID:            jobID,
		CondorJobEventID: condorJobEventID,
	}
	return je, nil
}

// UpdateLastCondorJobEvent modifies the record that tells what the last event
// for a job was.
func (d *Databaser) UpdateLastCondorJobEvent(je *model.LastCondorJobEvent) (*model.LastCondorJobEvent, error) {
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

// UpsertLastCondorJobEvent updates the last model.CondorJobEvent for a job if it's
// already set, but will insert it if it isn't already set.
func (d *Databaser) UpsertLastCondorJobEvent(jobEventID, jobID string) (string, error) {
	je, err := d.GetLastCondorJobEvent(jobID)
	if err == sql.ErrNoRows {
		le := &model.LastCondorJobEvent{
			JobID:            jobID,
			CondorJobEventID: jobEventID,
		}
		leID, err := d.InsertLastCondorJobEvent(le)
		if err != nil {
			logger.Printf("Error inserting last condor job event: %s", err)
			return "", err
		}
		return leID, err
	}
	je.CondorJobEventID = jobEventID
	updated, err := d.UpdateLastCondorJobEvent(je)
	if err != nil {
		logger.Printf("Error updating the last condor job event: %s", err)
		return "", err
	}
	return updated.JobID, nil
}

// InsertCondorJobStopRequest adds a record of a job stop request.
func (d *Databaser) InsertCondorJobStopRequest(jr *model.CondorJobStopRequest) (string, error) {
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
			$4
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
	DELETE FROM condor_job_stop_requests WHERE id = cast($1 as uuid)
	`
	_, err := d.db.Exec(query, uuid)
	if err != nil {
		return err
	}
	return nil
}

// GetCondorJobStopRequest returns the record of a job stop request.
func (d *Databaser) GetCondorJobStopRequest(uuid string) (*model.CondorJobStopRequest, error) {
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
	jr := &model.CondorJobStopRequest{
		ID:            id,
		JobID:         jobID,
		Username:      username,
		DateRequested: dateRequested,
		Reason:        reason,
	}
	return jr, nil
}

// UpdateCondorJobStopRequest updates the record of a job stop request.
func (d *Databaser) UpdateCondorJobStopRequest(jr *model.CondorJobStopRequest) (*model.CondorJobStopRequest, error) {
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
		jr.ID,
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

// InsertCondorJobDep adds a job dependency to the database.
func (d *Databaser) InsertCondorJobDep(jd *model.CondorJobDep) error {
	query := `
	INSERT INTO condor_job_deps (
		successor_id,
		predecessor_id
	) VALUES (
		cast($1 as uuid),
		cast($2 as uuid)
	)
	`
	_, err := d.db.Exec(
		query,
		jd.SuccessorID,
		jd.PredecessorID,
	)
	if err != nil {
		return err
	}
	return nil
}

// GetPredecessors will return a []model.JobRecord containing the model.JobRecords for jobs
// that are predecessors of the job whose ID is passed in.
func (d *Databaser) GetPredecessors(successor string) ([]model.JobRecord, error) {
	query := `
	SELECT successor_id,
	       predecessor_id
	  FROM condor_job_deps
	 WHERE successor_id = cast($1 as uuid)
	`
	rows, err := d.db.Query(query, successor)
	if err != nil {
		return nil, err
	}
	var retval []model.JobRecord
	for rows.Next() {
		var successorID string
		var predecessorID string
		err := rows.Scan(&successorID, &predecessorID)
		if err != nil {
			return nil, err
		}
		record, err := d.GetJob(predecessorID)
		if err != nil {
			return nil, err
		}
		retval = append(retval, *record)
	}
	return retval, nil
}

// GetSuccessors returns a []model.JobRecord of all jobs that are successors of the
// job whose ID is passed into the function.
func (d *Databaser) GetSuccessors(predecessor string) ([]model.JobRecord, error) {
	query := `
	SELECT successor_id,
	       predecessor_id
	  FROM condor_job_deps
	 WHERE predecessor_id = cast($1 as uuid)
	`
	rows, err := d.db.Query(query, predecessor)
	if err != nil {
		return nil, err
	}
	var retval []model.JobRecord
	for rows.Next() {
		var successorID string
		var predecessorID string
		err := rows.Scan(&successorID, &predecessorID)
		if err != nil {
			return nil, err
		}
		record, err := d.GetJob(successorID)
		if err != nil {
			return nil, err
		}
		retval = append(retval, *record)
	}
	return retval, nil
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
