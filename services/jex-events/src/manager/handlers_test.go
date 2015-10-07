package manager

import (
	"api"
	"configurate"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"logcabin"
	"messaging"
	"model"
	"os"
	"testing"

	"github.com/streadway/amqp"
)

func JSONData() ([]byte, error) {
	f, err := os.Open("../test/test_submission.json")
	if err != nil {
		return nil, err
	}
	c, err := ioutil.ReadAll(f)
	if err != nil {
		return nil, err
	}
	return c, err
}

var (
	s      *model.Job
	data   []byte
	l      = logcabin.New()
	client *messaging.Client
	db     *Databaser
)

func AMQPURI() string {
	uri := os.Getenv("RABBIT_PORT_5672_TCP_ADDR")
	if uri == "" {
		return uri
	}
	return fmt.Sprintf("amqp://guest:guest@%s:5672/", uri)
}

func _inittests(t *testing.T, memoize bool) *model.Job {
	if s == nil || !memoize {
		var err error
		configurate.Init("../test/test_config.yaml")
		configurate.C.Set("condor.run_on_nfs", true)
		configurate.C.Set("condor.nfs_base", "/path/to/base")
		configurate.C.Set("irods.base", "/path/to/irodsbase")
		configurate.C.Set("irods.host", "hostname")
		configurate.C.Set("irods.port", "1247")
		configurate.C.Set("irods.user", "user")
		configurate.C.Set("irods.pass", "pass")
		configurate.C.Set("irods.zone", "test")
		configurate.C.Set("irods.resc", "")
		configurate.C.Set("condor.log_path", "/path/to/logs")
		configurate.C.Set("condor.porklock_tag", "test")
		configurate.C.Set("condor.filter_files", "foo,bar,baz,blippy")
		configurate.C.Set("condor.request_disk", "0")
		data, err = JSONData()
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		s, err = model.NewFromData(data)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		PATH := fmt.Sprintf("../test/:%s", os.Getenv("PATH"))
		err = os.Setenv("PATH", PATH)
		if err != nil {
			t.Error(err)
		}
		connString, err := ConnString()
		if err == nil {
			db, err = NewDatabaser(connString)
			if err != nil {
				t.Error(err)
				t.Fail()
			}
		}
		uri := AMQPURI()
		if uri != "" {
			client = messaging.NewClient(uri)
			client.SetupPublishing(api.JobsExchange)
		}
	}
	return s
}

func inittests(t *testing.T) *model.Job {
	return _inittests(t, true)
}

func TestNewCommandsHandler(t *testing.T) {
	inittests(t)
	if db == nil || client == nil {
		return
	}
	actual := NewCommandsHandler(client, db)
	if actual == nil {
		t.Error("NewCommandsHandler returned nil")
		t.Fail()
	}
}

func TestCommandsHandle(t *testing.T) {
	inittests(t)
	if db == nil || client == nil {
		return
	}
	handler := NewCommandsHandler(client, db)
	launch := &api.JobRequest{
		Job:     s,
		Command: api.Launch,
		Message: "nothing",
		Version: 0,
	}
	body, err := json.Marshal(launch)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	delivery := &amqp.Delivery{
		Body: body,
	}
	handler.Handle(*delivery)
	job, err := db.GetJobByInvocationID(s.InvocationID)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if job.InvocationID != s.InvocationID {
		t.Error("InvocationID's didn't match")
	}
	log.Printf(fmt.Sprintf("-----------------------------------------------------%s\n", job.Submitter))
	if job.Submitter != s.Submitter {
		t.Error("Submitters didn't match")
	}
	err = db.DeleteJob(job.ID)
	if err != nil {
		t.Error(err)
	}
}

func TestNewStopsHandler(t *testing.T) {
	inittests(t)
	if db == nil || client == nil {
		return
	}
	handler := NewStopsHandler(db)
	stop := &api.StopRequest{
		Reason:       "test reason",
		Username:     s.Submitter,
		Version:      0,
		InvocationID: s.InvocationID,
	}
	body, err := json.Marshal(stop)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	delivery := &amqp.Delivery{
		Body: body,
	}
	jobID, err := db.InsertJob(s)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	handler.Handle(*delivery)
	record, err := db.GetStopRequestByInvID(s.InvocationID)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if record.JobID != jobID {
		t.Error("Job.ID fields didn't match")
	}
	if record.Reason != stop.Reason {
		t.Errorf("Reason was %s instead of %s", record.Reason, stop.Reason)
	}
	if record.Username != stop.Username {
		t.Errorf("Username was %s instead of %s", record.Username, stop.Username)
	}
}
