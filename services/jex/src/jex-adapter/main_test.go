package main

import (
	"bytes"
	"configurate"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"logcabin"
	"messaging"
	"model"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"

	"github.com/streadway/amqp"
)

var (
	s *model.Job
	l = logcabin.New()
)

func shouldrun() bool {
	if os.Getenv("RABBIT_PORT_5672_TCP_ADDR") != "" {
		return true
	}
	return false
}

func uri() string {
	addr := os.Getenv("RABBIT_PORT_5672_TCP_ADDR")
	port := os.Getenv("RABBIT_PORT_5672_TCP_PORT")
	return fmt.Sprintf("amqp://guest:guest@%s:%s/", addr, port)
}

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

func _inittests(t *testing.T, memoize bool) *model.Job {
	if s == nil || !memoize {
		configurate.Init("../test/test_config.yaml")
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
		data, err := JSONData()
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		s, err = model.NewFromData(data)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
	}
	return s
}

func inittests(t *testing.T) *model.Job {
	return _inittests(t, true)
}

func TestGetHome(t *testing.T) {
	req, err := http.NewRequest("GET", "http://for-a-test.org", nil)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	recorder := httptest.NewRecorder()
	home(recorder, req)
	actual := recorder.Body.String()
	expected := "Welcome to the JEX.\n"
	if actual != expected {
		t.Errorf("home() returned %s instead of %s", actual, expected)
	}
}

func TestStop(t *testing.T) {
	if !shouldrun() {
		return
	}
	invID := "test-invocation-id"
	stopKey := fmt.Sprintf("%s.%s", messaging.StopsKey, invID)
	exitChan := make(chan int)
	client = messaging.NewClient(uri())
	defer client.Close()
	client.AddConsumer(messaging.JobsExchange, "test_stop", stopKey, func(d amqp.Delivery) {
		d.Ack(false)
		stopMsg := &messaging.StopRequest{}
		err := json.Unmarshal(d.Body, stopMsg)
		if err != nil {
			t.Error(err)
		}
		actual := stopMsg.Reason
		expected := "User request"
		if actual != expected {
			t.Errorf("messaging.StopRequest.Reason was %s instead of %s", actual, expected)
		}
		actual = stopMsg.Username
		expected = "system"
		if actual != expected {
			t.Errorf("messaging.StopRequest.Username was %s instead of %s", actual, expected)
		}
		actual = stopMsg.InvocationID
		expected = invID
		if actual != expected {
			t.Errorf("messaging.StopRequest.InvocationID was %s instead of %s", actual, expected)
		}
		exitChan <- 1
	})
	client.SetupPublishing(messaging.JobsExchange)
	go client.Listen()
	time.Sleep(100 * time.Millisecond)
	requestURL := fmt.Sprintf("http://for-a-test.org/stop/%s", invID)
	request, err := http.NewRequest("DELETE", requestURL, nil)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	recorder := httptest.NewRecorder()
	NewRouter().ServeHTTP(recorder, request)
	if recorder.Code != 200 {
		t.Errorf("stop() didn't return a 200 status code: %d", recorder.Code)
	}
	<-exitChan
}

func TestLaunch(t *testing.T) {
	if !shouldrun() {
		return
	}
	job := inittests(t)
	exitChan := make(chan int)
	client = messaging.NewClient(uri())
	defer client.Close()
	client.AddConsumer(messaging.JobsExchange, "test_launch", messaging.LaunchesKey, func(d amqp.Delivery) {
		d.Ack(false)
		launch := &messaging.JobRequest{}
		err := json.Unmarshal(d.Body, launch)
		if err != nil {
			t.Error(err)
		}
		actual := launch.Job.Description
		expected := job.Description
		if actual != expected {
			t.Errorf("Description was %s instead of %s", actual, expected)
		}
		actual = launch.Job.InvocationID
		expected = job.InvocationID
		if actual != expected {
			t.Errorf("InvocationID was %s instead of %s", actual, expected)
		}
		exitChan <- 1
	})
	client.SetupPublishing(messaging.JobsExchange)
	go client.Listen()
	time.Sleep(100 * time.Millisecond)
	marshalledJob, err := json.Marshal(job)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	request, err := http.NewRequest("POST", "http://for-a-test.org/", bytes.NewReader(marshalledJob))
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	recorder := httptest.NewRecorder()
	NewRouter().ServeHTTP(recorder, request)
	if recorder.Code != 200 {
		t.Errorf("launch() didn't return a 200 status code: %d", recorder.Code)
	}
	<-exitChan
}

func TestPreview(t *testing.T) {
	job := inittests(t)
	params := job.Steps[0].Config.Params
	previewer := &Previewer{
		Params: model.PreviewableStepParam(params),
	}
	marshalledPreviewer, err := json.Marshal(previewer)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	request, err := http.NewRequest("POST", "http://for-a-test.org/arg-preview", bytes.NewReader(marshalledPreviewer))
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	recorder := httptest.NewRecorder()
	NewRouter().ServeHTTP(recorder, request)
	if recorder.Code != 200 {
		t.Errorf("preview() didn't return a 200 status code: %d", recorder.Code)
	}
	returnedPreview := &PreviewerReturn{}
	body, err := ioutil.ReadAll(recorder.Body)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = json.Unmarshal(body, returnedPreview)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	actual := returnedPreview.Params
	expected := model.PreviewableStepParam(params).String()
	if actual != expected {
		t.Errorf("param preview was %s instead of %s", actual, expected)
	}
}
