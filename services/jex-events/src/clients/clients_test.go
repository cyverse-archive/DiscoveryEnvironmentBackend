package clients

import (
	"encoding/json"
	"fmt"
	"model"
	"net/http"
	"testing"

	"github.com/facebookgo/freeport"
	"github.com/gorilla/mux"
)

func TestNewJEXEventsClient(t *testing.T) {
	cl, err := NewJEXEventsClient("http://example.org/path/")
	if err != nil {
		t.Error(err)
	}
	if cl.URL == nil {
		t.Errorf("NewJEXEventsClient returned a client with a nil URL field")
	}
	if cl.URL.Path != "/path/" {
		t.Errorf("JEXEventsClient.URL.Path was %s instead of /path/", cl.URL.Path)
	}
	if cl.URL.Host != "example.org" {
		t.Errorf("JEXEventsClient.URL.Host was %s instead of example.org", cl.URL.Host)
	}
	if cl.URL.Scheme != "http" {
		t.Errorf("JEXEventsClient.URL.Scheme was %s instead of http", cl.URL.Scheme)
	}
}

func TestJobRecord(t *testing.T) {
	expected := &model.Job{
		CondorID:     "10000",
		Submitter:    "test_this_is_a_test",
		AppID:        "c7f05682-23c8-4182-b9a2-e09650a5f49b",
		InvocationID: "00000000-0000-0000-0000-000000000000",
	}

	r := mux.NewRouter()
	r.HandleFunc("/invocations/{uuid}", func(w http.ResponseWriter, r *http.Request) {
		data, err := json.Marshal(expected)
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

	go server.ListenAndServe() //evil, evil, evil

	url := fmt.Sprintf("http://127.0.0.1:%d", p)
	cl, err := NewJEXEventsClient(url)
	if err != nil {
		t.Error(err)
		t.Fail()
	}

	actual, err := cl.JobRecord("00000000-0000-0000-0000-000000000000")
	if err != nil {
		t.Error(err)
	}
	if actual.CondorID != expected.CondorID {
		t.Errorf("CondorID was %s instead of %s", actual.CondorID, expected.CondorID)
	}
	if actual.Submitter != expected.Submitter {
		t.Errorf("Submitter was %s instead of %s", actual.Submitter, expected.Submitter)
	}
	if actual.AppID != expected.AppID {
		t.Errorf("AppID was %s instead of %s", actual.AppID, expected.AppID)
	}
}
