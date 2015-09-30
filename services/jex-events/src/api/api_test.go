package jexapi

import (
	"bytes"
	"configurate"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"logcabin"
	"model"
	"net/http"
	"net/http/httptest"
	"os"
	"path"
	"submissions"
	"testing"
	"time"

	"github.com/facebookgo/freeport"
	"github.com/gorilla/mux"
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
	l = logcabin.New()
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
	r := mux.NewRouter()
	r.HandleFunc("/jobs", func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte{})
	})
	p, err := freeport.Get()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	s := &http.Server{
		Addr:    fmt.Sprintf(":%d", p),
		Handler: r,
	}
	go s.ListenAndServe() //evil, evil, evil

	c.JEXEvents = fmt.Sprintf("http://127.0.0.1:%d", p)

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

func TestStopHandler(t *testing.T) {
	jr := &model.JobRecord{
		CondorID:     "10000",
		Submitter:    "test_this_is_a_test",
		AppID:        "c7f05682-23c8-4182-b9a2-e09650a5f49b",
		InvocationID: "00000000-0000-0000-0000-000000000000",
	}
	r := mux.NewRouter()
	r.HandleFunc("/invocations/{uuid}", func(w http.ResponseWriter, r *http.Request) {
		data, err := json.Marshal(jr)
		if err != nil {
			t.Error(err)
		}
		w.Write(data)
	})
	p, err := freeport.Get()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	server := &http.Server{
		Addr:    fmt.Sprintf(":%d", p),
		Handler: r,
	}
	go server.ListenAndServe()
	time.Sleep(1000 * time.Millisecond)
	c.JEXEvents = fmt.Sprintf("http://127.0.0.1:%d", p)
	inittests(t)
	r2 := mux.NewRouter()
	r2.HandleFunc("/stop/{uuid}", stopHandler)
	p2, err := freeport.Get()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	server2 := &http.Server{
		Addr:    fmt.Sprintf(":%d", p2),
		Handler: r2,
	}
	go server2.ListenAndServe() //even more evil, evil, evil
	time.Sleep(1000 * time.Millisecond)
	delete := fmt.Sprintf("http://127.0.0.1:%d/stop/%s", p2, jr.InvocationID)
	request, err := http.NewRequest("DELETE", delete, nil)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	response, err := http.DefaultClient.Do(request)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if response.StatusCode != 200 {
		t.Errorf("stopHandler returned a status code of %d", response.StatusCode)
	}
}
