package submissions

import (
	"configurate"
	"fmt"
	"io/ioutil"
	"log"
	"logcabin"
	"os"
	"path"
	"strings"
	"testing"
	"time"
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

var (
	s *Submission
	c = &configurate.Configuration{}
	l = log.New(logcabin.LoggerFunc(logcabin.LogWriter), "", log.Lshortfile)
)

func _inittests(t *testing.T, memoize bool) *Submission {
	if s == nil || !memoize {
		c.RunOnNFS = true
		c.NFSBase = "/path/to/base"
		c.IRODSBase = "/path/to/irodsbase"
		c.CondorLogPath = "/path/to/logs"
		Init(c, l)
		data, err := JSONData()
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		s, err = NewFromData(data)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
	}
	return s
}

func inittests(t *testing.T) *Submission {
	return _inittests(t, true)
}

func TestJSONParsing(t *testing.T) {
	inittests(t)
}

func TestNFSBase(t *testing.T) {
	s := inittests(t)
	if s.NFSBase != "/path/to/base" {
		t.Errorf("The nfs base directory was set to '%s' instead of '/path/to/base'", s.NFSBase)
	}
}

func TestIRODSBase(t *testing.T) {
	s := inittests(t)
	if s.IRODSBase != "/path/to/irodsbase" {
		t.Errorf("The IRODS base directory was set to '%s' instead of '/path/to/irodsbase'", s.IRODSBase)
	}
}

func TestRunOnNFS(t *testing.T) {
	s := inittests(t)
	if !s.RunOnNFS {
		t.Error("RunOnNFS was false when it should have been true")
	}
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
	if s.Name != "Word_Count_analysis1__" {
		t.Errorf("The name field was '%s' instead of 'Word_Count_analysis1__'", s.Name)
	}
}

func TestUsername(t *testing.T) {
	s := inittests(t)
	if s.Username != "wregglej_this_is_a_test" {
		t.Errorf("The username was '%s' instead of 'wregglej_this_is_a_test'", s.Username)
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

func TestStepContainerID(t *testing.T) {
	s := inittests(t)
	container := s.Steps[0].Component.Container
	if container.ID != "16fd2a16-3ac6-11e5-a25d-2fa4b0893ef1" {
		t.Errorf("The step's container ID was '%s' when it should have been '16fd2a16-3ac6-11e5-a25d-2fa4b0893ef1'", container.ID)
	}
}

func TestStepContainerImageID(t *testing.T) {
	s := inittests(t)
	image := s.Steps[0].Component.Container.Image
	if image.ID != "fc210a84-f7cd-4067-939c-a68ec3e3bd2b" {
		t.Errorf("The container image ID was '%s' when it should have been 'fc210a84-f7cd-4067-939c-a68ec3e3bd2b'", image.ID)
	}
}

func TestStepContainerImageURL(t *testing.T) {
	s := inittests(t)
	image := s.Steps[0].Component.Container.Image
	if image.URL != "https://registry.hub.docker.com/u/discoenv/backwards-compat" {
		t.Errorf("The container image URL was '%s' when it should have been 'https://registry.hub.docker.com/u/discoenv/backwards-compat'", image.URL)
	}
}

func TestStepContainerImageTag(t *testing.T) {
	s := inittests(t)
	image := s.Steps[0].Component.Container.Image
	if image.Tag != "latest" {
		t.Errorf("The container image tag was '%s' when it should have been 'latest'", image.Tag)
	}
}

func TestStepContainerImageName(t *testing.T) {
	s := inittests(t)
	image := s.Steps[0].Component.Container.Image
	if image.Name != "gims.iplantcollaborative.org:5000/backwards-compat" {
		t.Errorf("The container image name was '%s' when it should have been 'gims.iplantcollaborative.org:5000/backwards-compat'", image.Name)
	}
}

func TestStepEnvironmentLength(t *testing.T) {
	s := inittests(t)
	env := s.Steps[0].Environment
	envlen := len(env)
	if envlen != 2 {
		t.Errorf("The number of env vars is '%d' when it should have been '2'", envlen)
	}
}

func TestStepEnvironment(t *testing.T) {
	s := inittests(t)
	env := s.Steps[0].Environment
	if env["food"] != "banana" {
		t.Errorf("The env var 'food' was set to '%s' instead of 'banana'", env["food"])
	}
}

func TestStepEnvironment2(t *testing.T) {
	s := inittests(t)
	env := s.Steps[0].Environment
	if env["foo"] != "bar" {
		t.Errorf("The env var 'foo' was set to '%s' instead of 'bar'", env["foo"])
	}
}

func TestStepConfig(t *testing.T) {
	s := inittests(t)
	config := s.Steps[0].Config
	inputlen := len(config.Input)
	outputlen := len(config.Output)
	paramslen := len(config.Params)
	if inputlen != 1 {
		t.Errorf("The number of inputs was '%d' when it should have been '1'", inputlen)
	}
	if outputlen != 2 {
		t.Errorf("The number of outputs was '%d' when it should have been '2'", outputlen)
	}
	if paramslen != 2 {
		t.Errorf("The number of params was '%d' when it should have been '2'", paramslen)
	}
}

func TestConfigInputID(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Input[0]
	if input.ID != "2f58fce9-8183-4ab5-97c4-970592d1c35a" {
		t.Errorf("The input ID was '%s' when it should have been '2f58fce9-8183-4ab5-97c4-970592d1c35a'", input.ID)
	}
}

func TestConfigInputMultiplicity(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Input[0]
	if input.Multiplicity != "single" {
		t.Errorf("The input multiplicity was '%s' when it should have been 'single'", input.Multiplicity)
	}
}

func TestConfigInputName(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Input[0]
	if input.Name != "Acer-tree.txt" {
		t.Errorf("The input name was '%s' when it should have been 'Acer-tree.txt'", input.Name)
	}
}

func TestConfigInputProperty(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Input[0]
	if input.Property != "Acer-tree.txt" {
		t.Errorf("The input property was '%s' when it should have been 'Acer-tree.txt'", input.Name)
	}
}

func TestConfigInputRetain(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Input[0]
	if !input.Retain {
		t.Error("The input property was false when it should have been true")
	}
}

func TestConfigInputType(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Input[0]
	if input.Type != "FileInput" {
		t.Errorf("The input type was '%s' when it should have been 'FileInput'", input.Type)
	}
}

func TestConfigInputValue(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Input[0]
	if input.Value != "/iplant/home/wregglej/Acer-tree.txt" {
		t.Errorf("The input value was '%s' when it should have been '/iplant/home/wregglej/Acer-tree.txt'", input.Value)
	}
}

func TestConfigOutput0Multiplicity(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[0]
	if output.Multiplicity != "single" {
		t.Errorf("The output multiplicity was '%s' when it should have been 'single'", output.Multiplicity)
	}
}

func TestConfigOutput0Name(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[0]
	if output.Name != "wc_out.txt" {
		t.Errorf("The output name was '%s' when it should have been 'wc_out.txt'", output.Name)
	}
}

func TestConfigOutput0Property(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[0]
	if output.Property != "wc_out.txt" {
		t.Errorf("The output property was '%s' when it should have been 'wc_out.txt'", output.Property)
	}
}

func TestConfigOutput0QualID(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[0]
	if output.QualID != "67781636-854a-11e4-b715-e70c4f8db0dc_e7721c78-56c9-41ac-8ff5-8d46093f1fb1" {
		t.Errorf("The output qual-id was '%s' when it should have been '67781636-854a-11e4-b715-e70c4f8db0dc_e7721c78-56c9-41ac-8ff5-8d46093f1fb1'", output.QualID)
	}
}

func TestConfigOutput0Retain(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[0]
	if !output.Retain {
		t.Errorf("The output retain was false when it should have been true")
	}
}

func TestConfigOutput0Type(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[0]
	if output.Type != "File" {
		t.Errorf("The output type was '%s' when it should have been 'File'", output.Type)
	}
}

func TestConfigOutput1Multiplicity(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[1]
	if output.Multiplicity != "collection" {
		t.Errorf("The output multiplicity was '%s' when it should have been 'collection'", output.Multiplicity)
	}
}

func TestConfigOutput1Name(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[1]
	if output.Name != "logs" {
		t.Errorf("The output name was '%s' when it should have been 'logs'", output.Name)
	}
}

func TestConfigOutput1Property(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[1]
	if output.Property != "logs" {
		t.Errorf("The output property was '%s' when it should have been 'logs'", output.Property)
	}
}

func TestConfigOutput1Retain(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[1]
	if !output.Retain {
		t.Errorf("The output retain was false when it should have been true")
	}
}

func TestConfigOutput1Type(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Output[1]
	if output.Type != "File" {
		t.Errorf("The output type was '%s' when it should have been 'File'", output.Type)
	}
}

func TestConfigParams0ID(t *testing.T) {
	s := inittests(t)
	params := s.Steps[0].Config.Params[0]
	if params.ID != "e7721c78-56c9-41ac-8ff5-8d46093f1fb1" {
		t.Errorf("The param ID was '%s' when it should have been 'e7721c78-56c9-41ac-8ff5-8d46093f1fb1'", params.ID)
	}
}

func TestConfigParams0Name(t *testing.T) {
	s := inittests(t)
	params := s.Steps[0].Config.Params[0]
	if params.Name != "param0" {
		t.Errorf("The param name was '%s' when it should have been 'param0'", params.Name)
	}
}

func TestConfigParams0Order(t *testing.T) {
	s := inittests(t)
	params := s.Steps[0].Config.Params[0]
	if params.Order != 1 {
		t.Errorf("The param order was '%d' when it should have been '1'", params.Order)
	}
}

func TestConfigParams0Value(t *testing.T) {
	s := inittests(t)
	params := s.Steps[0].Config.Params[0]
	if params.Value != "wc_out.txt" {
		t.Errorf("The param value was '%s' when it should have been 'wc_out.txt'", params.Value)
	}
}

func TestConfigParams1ID(t *testing.T) {
	s := inittests(t)
	params := s.Steps[0].Config.Params[1]
	if params.ID != "2f58fce9-8183-4ab5-97c4-970592d1c35a" {
		t.Errorf("The param ID was '%s' when it should have been '2f58fce9-8183-4ab5-97c4-970592d1c35a'", params.ID)
	}
}

func TestConfigParams1Name(t *testing.T) {
	s := inittests(t)
	params := s.Steps[0].Config.Params[1]
	if params.Name != "param1" {
		t.Errorf("The param name was '%s' when it should have been 'param1'", params.Name)
	}
}

func TestConfigParams1Order(t *testing.T) {
	s := inittests(t)
	params := s.Steps[0].Config.Params[1]
	if params.Order != 2 {
		t.Errorf("The param order was '%d' when it should have been '2'", params.Order)
	}
}

func TestConfigParams1Value(t *testing.T) {
	s := inittests(t)
	params := s.Steps[0].Config.Params[1]
	if params.Value != "Acer-tree.txt" {
		t.Errorf("The param value was '%s' when it should have been 'Acer-tree.txt'", params.Value)
	}
}

func TestDirname(t *testing.T) {
	s := _inittests(t, false)
	s.NowDate = time.Now().Format(nowfmt)
	expected := fmt.Sprintf("%s-%s", s.Name, s.NowDate)
	actual := s.DirectoryName()
	if actual != expected {
		t.Errorf("Dirname() returned '%s' when it should have returned '%s'", actual, expected)
	}
}

func TestWorkingDir(t *testing.T) {
	s := _inittests(t, false)
	s.NowDate = time.Now().Format(nowfmt)
	expected := fmt.Sprintf("%s/", path.Join(s.NFSBase, s.Username, s.DirectoryName()))
	actual := s.WorkingDirectory()
	if actual != expected {
		t.Errorf("WorkingDir() returned '%s' when it should have returned '%s'", actual, expected)
	}
}

func TestCondorLogDir(t *testing.T) {
	s := _inittests(t, false)
	s.NowDate = time.Now().Format(nowfmt)
	expected := fmt.Sprintf("%s/", path.Join(c.CondorLogPath, s.Username, s.DirectoryName()))
	actual := s.CondorLogDirectory()
	if actual != expected {
		t.Errorf("CondorLogDir() returned '%s' when it should have returned '%s'", actual, expected)
	}
}

func TestIRODSConfig(t *testing.T) {
	s := _inittests(t, false)
	s.NowDate = time.Now().Format(nowfmt)
	expected := path.Join(s.WorkingDirectory(), "logs", "irods-config")
	actual := s.IRODSConfig()
	if actual != expected {
		t.Errorf("IRODSConfig() returned '%s' when it should have returned '%s'", actual, expected)
	}
}

func TestOutputDirectory1(t *testing.T) {
	s := _inittests(t, false)
	s.OutputDir = ""
	expected := path.Join(s.IRODSBase, s.Username, "analyses", s.DirectoryName())
	actual := s.OutputDirectory()
	if actual != expected {
		t.Errorf("OutputDirectory() returned '%s' when it should have returned '%s'", actual, expected)
	}
}

func TestOutputDirectory2(t *testing.T) {
	s := _inittests(t, false)
	expected := path.Join(s.OutputDir, s.DirectoryName())
	actual := s.OutputDirectory()
	if actual != expected {
		t.Errorf("OutputDirectory() returned '%s' when it should have returned '%s'", actual, expected)
	}
}

func TestOutputDirectory3(t *testing.T) {
	s := _inittests(t, false)
	s.CreateOutputSubdir = false
	expected := strings.TrimSuffix(s.OutputDir, "/")
	actual := s.OutputDirectory()
	if actual != expected {
		t.Errorf("OutputDirectory() returned '%s' when it should have returned '%s'", actual, expected)
	}
}
