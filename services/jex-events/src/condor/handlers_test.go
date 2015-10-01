package condor

import (
	"bytes"
	"configurate"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"model"
	"net/http"
	"net/http/httptest"
	"os"
	"path"
	"testing"
	"time"

	"github.com/facebookgo/freeport"
	"github.com/gorilla/mux"
)

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

	//Start up a fake jex events for the handler to communicate with.
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

	configurate.Config.JEXEvents = fmt.Sprintf("http://127.0.0.1:%d", p)
	time.Sleep(1000 * time.Millisecond) //Wait for the fake jex-events to start.

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
	parent := path.Join(configurate.Config.CondorLogPath, "test_this_is_a_test")
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
	s, err := model.NewFromData(data)
	if err != nil {
		t.Error(err)
	}
	postMap := make(map[string][]model.StepParam)
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
	//Start up a fake jex-events
	jr := &model.Job{
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
	time.Sleep(1000 * time.Millisecond) //wait for fake jex-events to start
	configurate.Config.JEXEvents = fmt.Sprintf("http://127.0.0.1:%d", p)

	// have to start up a server for stop handler in order to extract the uuid
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
