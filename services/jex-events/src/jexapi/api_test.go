package jexapi

import (
	"bytes"
	"configurate"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"logcabin"
	"net/http"
	"net/http/httptest"
	"os"
	"path"
	"submissions"
	"testing"
)

func JSONData() ([]byte, error) {
	f, err := os.Open("../submissions/test_submission.json")
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
	c = &configurate.Configuration{}
	l = log.New(logcabin.LoggerFunc(logcabin.LogWriter), "", log.Lshortfile)
)

func inittests(t *testing.T) {
	c.RunOnNFS = true
	c.NFSBase = "/path/to/base"
	c.IRODSBase = "/path/to/irodsbase"
	c.CondorLogPath = ""
	c.PorklockTag = "test"
	c.FilterFiles = "foo,bar,baz,blippy"
	c.RequestDisk = "0"
	submissions.Init(c, l)
	PATH := fmt.Sprintf("../submissions/:%s", os.Getenv("PATH"))
	err := os.Setenv("PATH", PATH)
	if err != nil {
		t.Error(err)
	}
	Init(c, l)
}

func TestRootHandler(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(rootHandler))
	defer server.Close()

	response, err := http.Get(server.URL)
	if err != nil {
		t.Error(err)
	}
	msg, err := ioutil.ReadAll(response.Body)
	response.Body.Close()
	if err != nil {
		t.Error(err)
	}
	actual := string(msg)
	expected := "Welcome to the JEX."
	if actual != expected {
		t.Errorf("rootHandler returned:\n%s\ninstead of:\n%s\n", actual, expected)
	}
}

func TestSubmissionHandler(t *testing.T) {
	inittests(t)
	server := httptest.NewServer(http.HandlerFunc(submissionHandler))
	defer server.Close()

	data, err := JSONData()
	if err != nil {
		t.Error(err)
	}
	buf := bytes.NewBuffer(data)
	response, err := http.Post(server.URL, "application/json", buf)
	if err != nil {
		t.Error(err)
	}
	msg, err := ioutil.ReadAll(response.Body)
	response.Body.Close()
	if err != nil {
		t.Error(err)
	}
	actual := string(msg)
	expected := `{"sub_id":"10000"}`
	if actual != expected {
		t.Errorf("submissionHandler returned:\n%s\ninstead of:\n%s\n", actual, expected)
	}
	parent := path.Join(cfg.CondorLogPath, "test_this_is_a_test")
	err = os.RemoveAll(parent)
	if err != nil {
		t.Error(err)
	}
}

func TestParameterPreviewHandler(t *testing.T) {
	inittests(t)
	server := httptest.NewServer(http.HandlerFunc(parameterPreview))
	defer server.Close()

	data, err := JSONData()
	if err != nil {
		t.Error(err)
	}
	s, err := submissions.NewFromData(data)
	if err != nil {
		t.Error(err)
	}
	postMap := make(map[string][]submissions.StepParam)
	postMap["params"] = s.Steps[0].Config.Parameters()
	postData, err := json.Marshal(postMap)
	if err != nil {
		t.Error(err)
	}
	buf := bytes.NewBuffer(postData)
	response, err := http.Post(server.URL, "application/json", buf)
	if err != nil {
		t.Error(err)
	}
	msg, err := ioutil.ReadAll(response.Body)
	response.Body.Close()
	if err != nil {
		t.Error(err)
	}
	actual := string(msg)
	expected := "param1 'Acer-tree.txt' param0 'wc_out.txt'"
	if actual != expected {
		t.Errorf("parameterPreview returned:\n%s\ninstead of:\n%s\n", actual, expected)
	}
}
