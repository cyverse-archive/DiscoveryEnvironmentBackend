package submissions

import (
	"encoding/json"
	"io/ioutil"
	"os"
	"testing"
)

func JSONData() ([]byte, error) {
	f, err := os.Open("test_submission.json")
	if err != nil {
		return nil, err
	}
	c, err := ioutil.ReadAll(f)
	if err != nil {
		return nil, err
	}
	return c, err
}

var s *Submission

func inittests(t *testing.T) *Submission {
	if s == nil {
		data, err := JSONData()
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		err = json.Unmarshal(data, &s)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
	}
	return s
}

func TestJSONParsing(t *testing.T) {
	inittests(t)
}

func TestDescription(t *testing.T) {
	s := inittests(t)
	if s.Description != "this is a description" {
		t.Error("The description is wrong")
	}
}

func TestEmail(t *testing.T) {
	s := inittests(t)
	if s.Email != "wregglej@iplantcollaborative.org" {
		t.Error("The email is wrong")
	}
}
