package main

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"messaging"
	"model"
	"net"
	"os"
	"testing"
	"time"

	"github.com/streadway/amqp"
)

func shouldrun() bool {
	db := false
	rabbit := false
	if os.Getenv("DEDB_PORT_5432_TCP_ADDR") != "" {
		db = true
	} else {
		db = false
	}
	if os.Getenv("RABBIT_PORT_5672_TCP_ADDR") != "" {
		rabbit = true
	} else {
		rabbit = true
	}
	return db && rabbit
}

func rabbituri() string {
	addr := os.Getenv("RABBIT_PORT_5672_TCP_ADDR")
	port := os.Getenv("RABBIT_PORT_5672_TCP_PORT")
	return fmt.Sprintf("amqp://guest:guest@%s:%s/", addr, port)
}

func dburi() string {
	addr := os.Getenv("DEDB_PORT_5432_TCP_ADDR")
	port := os.Getenv("DEDB_PORT_5432_TCP_PORT")
	return fmt.Sprintf("postgres://de:notprod@%s:%s/de?sslmode=disable", addr, port)
}

func initdb(t *testing.T) *sql.DB {
	db, err := sql.Open("postgres", dburi())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = db.Ping()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	return db
}

func TestInsert(t *testing.T) {
	if !shouldrun() {
		return
	}
	db = initdb(t)
	defer db.Close()
	n := time.Now()
	actual, err := insert("RUNNING", "test-invocation-id", "test", "localhost", "127.0.0.1", n)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	rowCount, err := actual.RowsAffected()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	rows, err := db.Query("select status, message, sent_from, sent_from_hostname, sent_on from job_status_updates where external_id = 'test-invocation-id'")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	defer rows.Close()
	var (
		status, message, sentFromHostname string
		sentOn                            *time.Time
		sentFrom                          string
	)
	for rows.Next() {
		err := rows.Scan(&status, &message, &sentFrom, &sentFromHostname, &sentOn)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		if status != "RUNNING" {
			t.Errorf("status was %s instead of RUNNING", status)
		}
		if message != "test" {
			t.Errorf("message was %s instead of 'test'", message)
		}
		if sentFrom != "127.0.0.1" {
			t.Errorf("sentFrom was %s instead of '127.0.0.1'", sentFrom)
		}
		if sentFromHostname != "localhost" {
			t.Errorf("sentFromHostname was %s instead of 'localhost'", sentFromHostname)
		}
		if n.Equal(*sentOn) {
			t.Errorf("sentOn was %#v instead of %#v", sentOn, n)
		}
	}
	err = rows.Err()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if rowCount != 1 {
		t.Errorf("RowsAffected() should have returned 1: %d", rowCount)
	}
	_, err = db.Exec("DELETE FROM job_status_updates")
	if err != nil {
		t.Error(err)
	}
}

func TestMsg(t *testing.T) {
	if !shouldrun() {
		return
	}
	db = initdb(t)
	defer db.Close()
	me, err := os.Hostname()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	j := &model.Job{InvocationID: "test-invocation-id"}
	expected := &messaging.UpdateMessage{
		Job:     j,
		State:   "RUNNING",
		Message: "this is a test",
		Sender:  me,
	}
	m, err := json.Marshal(expected)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	d := amqp.Delivery{
		Body:      m,
		Timestamp: time.Now(),
	}
	msg(d)
	rows, err := db.Query("select status, message, sent_from, sent_from_hostname, sent_on from job_status_updates where external_id = 'test-invocation-id'")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	defer rows.Close()
	var (
		status, message, sentFromHostname string
		sentOn                            *time.Time
		sentFrom                          string
	)
	for rows.Next() {
		err := rows.Scan(&status, &message, &sentFrom, &sentFromHostname, &sentOn)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		if status != string(expected.State) {
			t.Errorf("status was %s instead of %s", status, expected.State)
		}
		if message != expected.Message {
			t.Errorf("message was %s instead of %s", message, expected.Message)
		}
		ips, err := net.LookupIP(expected.Sender)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		var expectedSentFrom string
		if len(ips) > 0 {
			expectedSentFrom = ips[0].String()
		} else {
			t.Error("Couldn't get ip address")
			t.Fail()
		}
		if sentFrom != expectedSentFrom {
			t.Errorf("sentFrom was %s instead of %s", sentFrom, expectedSentFrom)
		}
		if sentFromHostname != me {
			t.Errorf("sentFromHostname was %s instead of %s", sentFromHostname, me)
		}
		if d.Timestamp.Equal(*sentOn) {
			t.Errorf("sentOn was %#v instead of %#v", sentOn, d.Timestamp)
		}
	}
	err = rows.Err()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	_, err = db.Exec("DELETE FROM job_status_updates")
	if err != nil {
		t.Error(err)
	}
}
