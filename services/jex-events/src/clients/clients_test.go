package clients

import (
	"encoding/json"
	"io/ioutil"
	"model"
	"net/http"
	"net/http/httptest"
	"testing"
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
	expected := &model.JobRecord{
		CondorID:     "10000",
		Submitter:    "test_this_is_a_test",
		AppID:        "c7f05682-23c8-4182-b9a2-e09650a5f49b",
		InvocationID: "00000000-0000-0000-0000-000000000000",
	}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		data, err := json.Marshal(expected)
		if err != nil {
			t.Error(err)
		}
		w.Write(data)
	}))
	defer server.Close()

	response, err := http.Get(server.URL)
	if err != nil {
		t.Error(err)
	}
	data, err := ioutil.ReadAll(response.Body)
	if err != nil {
		t.Error(err)
	}
	var actual model.JobRecord
	err = json.Unmarshal(data, &actual)
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
