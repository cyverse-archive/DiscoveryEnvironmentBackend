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
	DateSubmitted    string
	DateStarted      string
	DateCompleted    string
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
		DateSubmitted:    datesubmitted.Format(time.RFC822Z),
		DateStarted:      datestarted.Format(time.RFC822Z),
		DateCompleted:    datecompleted.Format(time.RFC822Z),
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
