package manager

import (
	"configurate"
	"fmt"
	"io/ioutil"
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
		}
	}
	return s
}

func inittests(t *testing.T) *model.Job {
	return _inittests(t, true)
}

func TestNewCommandsHandler(t *testing.T) {
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
	if db == nil || client == nil {
		return
	}
	handler := NewCommandsHandler(client, db)
	delivery := &amqp.Delivery{
		Body: data,
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
	err = db.DeleteJob(job.ID)
	if err != nil {
		t.Error(err)
	}
}
