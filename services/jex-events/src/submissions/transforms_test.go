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
		t.Errorf("The description was '%s' instead of 'this is a description'", s.Description)
	}
}

func TestEmail(t *testing.T) {
	s := inittests(t)
	if s.Email != "wregglej@iplantcollaborative.org" {
		t.Errorf("The email was '%s' instead of wregglej@iplantcollaborative.org", s.Email)
	}
}

func TestName(t *testing.T) {
	s := inittests(t)
	if s.Name != "Word_Count_analysis1" {
		t.Errorf("The name field was '%s' instead of 'Word_Count_analysis1'", s.Name)
	}
}

func TestUsername(t *testing.T) {
	s := inittests(t)
	if s.Username != "wregglej" {
		t.Errorf("The username was '%s' instead of 'wregglej'", s.Username)
	}
}

func TestAppID(t *testing.T) {
	s := inittests(t)
	if s.AppID != "c7f05682-23c8-4182-b9a2-e09650a5f49b" {
		t.Errorf("The app_id was '%s' instead of 'c7f05682-23c8-4182-b9a2-e09650a5f49b'", s.AppID)
	}
}

func TestCreateOutputSubdir(t *testing.T) {
	s := inittests(t)
	if !s.CreateOutputSubdir {
		t.Errorf("create_output_subdir was false when it should have been true")
	}
}

func TestRequestType(t *testing.T) {
	s := inittests(t)
	if s.RequestType != "submit" {
		t.Errorf("request_type was '%s' instead of 'submit'", s.RequestType)
	}
}

func TestAppDescription(t *testing.T) {
	s := inittests(t)
	if s.AppDescription != "this is an app description" {
		t.Errorf("app_description was '%s' instead of 'this is an app description'", s.AppDescription)
	}
}

func TestOutputDir(t *testing.T) {
	s := inittests(t)
	if s.OutputDir != "/iplant/home/wregglej/analyses/Word_Count_analysis1-2015-09-17-21-42-20.9" {
		t.Errorf("output_dir was '%s' instead of '/iplant/home/wregglej/analyses/Word_Count_analysis1-2015-09-17-21-42-20.9'", s.OutputDir)
	}
}

func TestWikiURL(t *testing.T) {
	s := inittests(t)
	if s.WikiURL != "https://pods.iplantcollaborative.org/wiki/display/DEapps/WordCount" {
		t.Errorf("wiki_url was '%s' instead of 'https://pods.iplantcollaborative.org/wiki/display/DEapps/WordCount'", s.WikiURL)
	}
}

func TestUUID(t *testing.T) {
	s := inittests(t)
	if s.UUID != "07b04ce2-7757-4b21-9e15-0b4c2f44be26" {
		t.Errorf("uuid was '%s' instead of '07b04ce2-7757-4b21-9e15-0b4c2f44be26'", s.UUID)
	}
}

func TestNotify(t *testing.T) {
	s := inittests(t)
	if !s.Notify {
		t.Errorf("notify was false instead of true")
	}
}

func TestExecutionTarget(t *testing.T) {
	s := inittests(t)
	if s.ExecutionTarget != "condor" {
		t.Errorf("execution_target was '%s' instead of 'condor'", s.ExecutionTarget)
	}
}

func TestAppName(t *testing.T) {
	s := inittests(t)
	if s.AppName != "Word Count" {
		t.Errorf("app_name was '%s' instead of 'Word Count'", s.AppName)
	}
}

func TestStepsCount(t *testing.T) {
	s := inittests(t)
	numSteps := len(s.Steps)
	if numSteps != 1 {
		t.Errorf("The number of steps was %d instead of 1", numSteps)
	}
}

func TestStepType(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.Type != "condor" {
		t.Errorf("The step type was '%s' instead of 'condor'", step.Type)
	}
}

func TestStepStdin(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.Stdin != "/path/to/stdin" {
		t.Errorf("The step's path to stdin was '%s' instead of '/path/to/stdin'", step.Stdin)
	}
}

func TestStepStdout(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.Stdout != "/path/to/stdout" {
		t.Errorf("The step's path to stdout was '%s' instead of '/path/to/stdout'", step.Stdout)
	}
}

func TestStepStderr(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.Stderr != "/path/to/stderr" {
		t.Errorf("The step's path to stderr was '%s' instead of '/path/to/stderr'", step.Stderr)
	}
}

func TestStepComponentType(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.Component.Type != "executable" {
		t.Errorf("The step's component type was '%s' when it should have been 'executable'", step.Component.Type)
	}
}

func TestStepComponentName(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.Component.Name != "wc_wrapper.sh" {
		t.Errorf("The step's component name was '%s' when it should have been 'wc_wrapper.sh'", step.Component.Name)
	}
}

func TestStepComponentLocation(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.Component.Location != "/usr/local3/bin/wc_tool-1.00" {
		t.Errorf("The step's component location was '%s' when it should have been '/usr/local3/bin/wc_tool-1.00'", step.Component.Location)
	}
}

func TestStepComponentDescription(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.Component.Description != "Word Count" {
		t.Errorf("The step's component description was '%s' when it should have been 'Word Count'", step.Component.Description)
	}
}
