package logcabin

import (
	"encoding/json"
	"io/ioutil"
	"os"
	"testing"
)

func TestNewLogMessage(t *testing.T) {
	m := NewLogMessage("foo")
	expected := "jex-events"
	if m.Service != expected {
		t.Errorf("LogMessage.Service was %s instead of %s", m.Service, expected)
	}
	if m.Artifact != expected {
		t.Errorf("LogMessage.Artifact was %s instead of %s", m.Artifact, expected)
	}
	expected = "org.iplantc"
	if m.Group != expected {
		t.Errorf("LogMessage.Group was %s instead of %s", m.Group, expected)
	}
	expected = "INFO"
	if m.Level != expected {
		t.Errorf("LogMessage.Level was %s instead of %s", m.Level, expected)
	}
	expected = "foo"
	if m.Message != expected {
		t.Errorf("LogMessage.Message was %s instead of %s", m.Message, expected)
	}
}

func TestLogWriter(t *testing.T) {
	original := os.Stdout
	r, w, err := os.Pipe()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	os.Stdout = w
	restore := func() {
		os.Stdout = original
	}
	defer restore()
	expected := "this is a test"
	_, err = LogWriter([]byte(expected))
	if err != nil {
		t.Error(err)
		os.Stdout = original
		t.Fail()
	}
	w.Close()
	var msg LogMessage
	actualBytes, err := ioutil.ReadAll(r)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = json.Unmarshal(actualBytes, &msg)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	actual := msg.Message
	if actual != expected {
		t.Errorf("LogWriter returned %s instead of %s", actual, expected)
	}

}
