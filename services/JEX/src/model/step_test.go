package model

import (
	"reflect"
	"testing"
)

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

func TestStepStdinPath(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.StdinPath != "/path/to/stdin" {
		t.Errorf("The step's path to stdin was '%s' instead of '/path/to/stdin'", step.StdinPath)
	}
}

func TestStepStdoutPath(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.StdoutPath != "/path/to/stdout" {
		t.Errorf("The step's path to stdout was '%s' instead of '/path/to/stdout'", step.StdoutPath)
	}
}

func TestStepStderrPath(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	if step.StderrPath != "/path/to/stderr" {
		t.Errorf("The step's path to stderr was '%s' instead of '/path/to/stderr'", step.StderrPath)
	}
}

func TestStepStdin(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	actual := step.Stdin()
	expected := "'/path/to/stdin'"
	if actual != expected {
		t.Errorf("Stdin() returned '%s' instead of '%s'", actual, expected)
	}
	step.StdinPath = ""
	actual = step.Stdin()
	expected = ""
	if actual != expected {
		t.Errorf("Stdin() returned '%s' instead of '%s'", actual, expected)
	}
	_inittests(t, false)
}

func TestStepStdout(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	actual := step.Stdout("foo")
	expected := "/path/to/stdout"
	if actual != expected {
		t.Errorf("Stdout() returned '%s' instead of '%s'", actual, expected)
	}
	step.StdoutPath = ""
	actual = step.Stdout("foo")
	expected = "logs/condor-stdout-foo"
	if actual != expected {
		t.Errorf("Stdout() returned '%s' instead of '%s'", actual, expected)
	}
	_inittests(t, false)
}

func TestStepStderr(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	actual := step.Stderr("foo")
	expected := "/path/to/stderr"
	if actual != expected {
		t.Errorf("Stderr() returned '%s' instead of '%s'", actual, expected)
	}
	step.StderrPath = ""
	actual = step.Stderr("foo")
	expected = "logs/condor-stderr-foo"
	if actual != expected {
		t.Errorf("Stderr() returned '%s' instead of '%s'", actual, expected)
	}
	_inittests(t, false)
}

func TestLogPath(t *testing.T) {
	s := inittests(t)
	step := s.Steps[0]
	actual := step.LogPath("parent", "suffix")
	expected := "parent/log-file-name"
	if actual != expected {
		t.Errorf("LogPath() returned '%s' instead of '%s'", actual, expected)
	}
	step.LogFile = ""
	actual = step.LogPath("parent", "suffix")
	expected = "parent/logs/condor-log-suffix"
	if actual != expected {
		t.Errorf("LogPath() returned '%s' instead of '%s'", actual, expected)
	}
	_inittests(t, false)
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

func TestEnvOptions(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].EnvOptions()
	expected := []string{"--env=\"food=banana\"", "--env=\"foo=bar\""}
	expected2 := []string{"--env=\"foo=bar\"", "--env=\"food=banana\""}
	if !reflect.DeepEqual(actual, expected) && !reflect.DeepEqual(actual, expected2) {
		if !reflect.DeepEqual(actual, expected) {
			t.Errorf("EnvOptions() returned '%#v' instead of '%#v'", actual, expected)
		}
		if !reflect.DeepEqual(actual, expected2) {
			t.Errorf("EnvOptions() returned '%#v' instead of '%#v'", actual, expected2)
		}
	}
	s.Steps[0].Environment = make(StepEnvironment)
	actual = s.Steps[0].EnvOptions()
	expected = []string{}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("EnvOptions() returned '%s' instead of '%s'", actual, expected)
	}
	_inittests(t, false)
}

func TestIsBackwardsCompatible(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].IsBackwardsCompatible()
	if !actual {
		t.Errorf("IsBackwardsCompatible() returned false")
	}
	s.Steps[0].Component.Container.Image.Name = "discoenv/test"
	actual = s.Steps[0].IsBackwardsCompatible()
	if actual {
		t.Errorf("IsBackwardsCompatible() returned true")
	}
	_inittests(t, false)
}

func TestBackwardsCompatibleOptions(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].BackwardsCompatibleOptions()
	expected := []string{"-v", "/usr/local2/:/usr/local2", "-v", "/usr/local3/:/usr/local3/", "-v", "/data2/:/data2/"}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("BackwardsCompatibleOptions() returned '%#v' instead of '%#v'", actual, expected)
	}
	s.Steps[0].Component.Container.Image.Name = "discoenv/test"
	actual = s.Steps[0].BackwardsCompatibleOptions()
	expected = []string{}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("BackwardsCompatibleOptions() returned '%#v' instead of '%#v'", actual, expected)
	}
	_inittests(t, false)
}

func TestExecutable(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Executable()
	expected := "/usr/local3/bin/wc_tool-1.00/wc_wrapper.sh"
	if actual != expected {
		t.Errorf("Executable() returned '%s' instead of '%s'", actual, expected)
	}
	s.Steps[0].Component.Container.Image.Name = "discoenv/test"
	actual = s.Steps[0].Executable()
	expected = ""
	if actual != expected {
		t.Errorf("Executable() returned '%s' instead of '%s'", actual, expected)
	}
	_inittests(t, false)
}

func TestArguments(t *testing.T) {
	s := inittests(t)
	s.Steps[0].Environment = make(StepEnvironment) // Removed the environment to save my sanity. It's unordered.
	actual := s.Steps[0].Arguments()
	expected := []string{
		"/usr/local3/bin/wc_tool-1.00/wc_wrapper.sh",
		"param1", "Acer-tree.txt", "param0", "wc_out.txt",
	}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("Arguments() returned:\n\t%#v\ninstead of:\n\t%#v", actual, expected)
	}
}

// func TestArguments(t *testing.T) {
// 	s := inittests(t)
// 	actual := s.Steps[0].Arguments("foo")
// 	expected := `run --rm -e IPLANT_USER -e IPLANT_EXECUTION_ID -v /usr/local2/:/usr/local2 -v /usr/local3/:/usr/local3/ -v /data2/:/data2/ -v $(pwd):/work -v /host/path1:/container/path1 -v /container/path2 --device=/host/path1:/container/path1 --device=/host/path2:/container/path2 --volumes-from=foo-vf-prefix1 --volumes-from=foo-vf-prefix2 --name test-name -w /work --memory=2048M --cpu-shares=2048 --net=none --entrypoint=/bin/true gims.iplantcollaborative.org:5000/backwards-compat:test /usr/local3/bin/wc_tool-1.00/wc_wrapper.sh param1 'Acer-tree.txt' param0 'wc_out.txt'`
// 	if actual != expected {
// 		t.Errorf("Arguments() returned:\n\t%s\ninstead of:\n\t%s", actual, expected)
// 	}
// 	_inittests(t, false)
// }

func TestStepConfig(t *testing.T) {
	s := inittests(t)
	config := s.Steps[0].Config
	inputlen := len(config.Inputs)
	outputlen := len(config.Outputs)
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
	input := s.Steps[0].Config.Inputs[0]
	if input.ID != "2f58fce9-8183-4ab5-97c4-970592d1c35a" {
		t.Errorf("The input ID was '%s' when it should have been '2f58fce9-8183-4ab5-97c4-970592d1c35a'", input.ID)
	}
}

func TestConfigInputMultiplicity(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	if input.Multiplicity != "single" {
		t.Errorf("The input multiplicity was '%s' when it should have been 'single'", input.Multiplicity)
	}
}

func TestConfigInputName(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	if input.Name != "Acer-tree.txt" {
		t.Errorf("The input name was '%s' when it should have been 'Acer-tree.txt'", input.Name)
	}
}

func TestConfigInputProperty(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	if input.Property != "Acer-tree.txt" {
		t.Errorf("The input property was '%s' when it should have been 'Acer-tree.txt'", input.Name)
	}
}

func TestConfigInputRetain(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	if !input.Retain {
		t.Error("The input property was false when it should have been true")
	}
}

func TestConfigInputType(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	if input.Type != "FileInput" {
		t.Errorf("The input type was '%s' when it should have been 'FileInput'", input.Type)
	}
}

func TestConfigInputValue(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	if input.Value != "/iplant/home/wregglej/Acer-tree.txt" {
		t.Errorf("The input value was '%s' when it should have been '/iplant/home/wregglej/Acer-tree.txt'", input.Value)
	}
}

func TestInputIRODSPath(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	actual := input.IRODSPath()
	expected := "/iplant/home/wregglej/Acer-tree.txt"
	if actual != expected {
		t.Errorf("IRODSPath() returned '%s' instead of '%s'", actual, expected)
	}
	input.Value = "/iplant/home/wregglej/Acer-tree"
	input.Multiplicity = "collection"
	actual = input.IRODSPath()
	expected = "/iplant/home/wregglej/Acer-tree/"
	if actual != expected {
		t.Errorf("IRODSPath() returned '%s' instead of '%s'", actual, expected)
	}
	_inittests(t, false)
}

func TestInputIdentifier(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	actual := input.Identifier("0-0")
	expected := "input-0-0"
	if actual != expected {
		t.Errorf("Identifier() returned %s instead of %s", actual, expected)
	}
}

func TestInputStdout(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	actual := input.Stdout("0-0")
	expected := "logs/logs-stdout-input-0-0"
	if actual != expected {
		t.Errorf("StepInput.Stdout() returned %s instead of %s", actual, expected)
	}
}

func TestInputStderr(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	actual := input.Stderr("0-0")
	expected := "logs/logs-stderr-input-0-0"
	if actual != expected {
		t.Errorf("StepInput.Stderr() returned %s instead of %s", actual, expected)
	}
}

func TestInputLogPath(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	actual := input.LogPath("parent", "0-0")
	expected := "parent/logs/logs-condor-input-0-0"
	if actual != expected {
		t.Errorf("StepInput.LogPath() returned %s instead of %s", actual, expected)
	}
}

func TestInputArguments(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	actual := input.Arguments("testuser", s.FileMetadata)
	expected := []string{
		"get",
		"--user", "testuser",
		"--source", "'/iplant/home/wregglej/Acer-tree.txt'",
		"--config", "irods-config",
		"-m", "'attr1,value1,unit1'",
		"-m", "'attr2,value2,unit2'",
		"-m", "'ipc-analysis-id,c7f05682-23c8-4182-b9a2-e09650a5f49b,UUID'",
		"-m", "'ipc-execution-id,07b04ce2-7757-4b21-9e15-0b4c2f44be26,UUID'",
	}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("Arguments() returned:\n\t%#v\ninstead of:\n\t%#v", actual, expected)
	}
}

func TestInputSource(t *testing.T) {
	s := inittests(t)
	input := s.Steps[0].Config.Inputs[0]
	actual := input.Source()
	expected := "Acer-tree.txt"
	if actual != expected {
		t.Errorf("Source() returned %s instead of %s", actual, expected)
	}
	input.Value = "/foo"
	input.Multiplicity = "collection"
	actual = input.Source()
	expected = "foo/"
	if actual != expected {
		t.Errorf("Source() returned %s instead of %s", actual, expected)
	}
	_inittests(t, false)
}

func TestConfigOutput0Multiplicity(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	if output.Multiplicity != "single" {
		t.Errorf("The output multiplicity was '%s' when it should have been 'single'", output.Multiplicity)
	}
}

func TestConfigOutput0Name(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	if output.Name != "wc_out.txt" {
		t.Errorf("The output name was '%s' when it should have been 'wc_out.txt'", output.Name)
	}
}

func TestConfigOutput0Property(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	if output.Property != "wc_out.txt" {
		t.Errorf("The output property was '%s' when it should have been 'wc_out.txt'", output.Property)
	}
}

func TestConfigOutput0QualID(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	if output.QualID != "67781636-854a-11e4-b715-e70c4f8db0dc_e7721c78-56c9-41ac-8ff5-8d46093f1fb1" {
		t.Errorf("The output qual-id was '%s' when it should have been '67781636-854a-11e4-b715-e70c4f8db0dc_e7721c78-56c9-41ac-8ff5-8d46093f1fb1'", output.QualID)
	}
}

func TestConfigOutput0Retain(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	if !output.Retain {
		t.Errorf("The output retain was false when it should have been true")
	}
}

func TestConfigOutput0Type(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	if output.Type != "File" {
		t.Errorf("The output type was '%s' when it should have been 'File'", output.Type)
	}
}

func TestConfigOutput1Multiplicity(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[1]
	if output.Multiplicity != "collection" {
		t.Errorf("The output multiplicity was '%s' when it should have been 'collection'", output.Multiplicity)
	}
}

func TestConfigOutput1Name(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[1]
	if output.Name != "logs" {
		t.Errorf("The output name was '%s' when it should have been 'logs'", output.Name)
	}
}

func TestConfigOutput1Property(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[1]
	if output.Property != "logs" {
		t.Errorf("The output property was '%s' when it should have been 'logs'", output.Property)
	}
}

func TestConfigOutput1Retain(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[1]
	if !output.Retain {
		t.Errorf("The output retain was false when it should have been true")
	}
}

func TestConfigOutput1Type(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[1]
	if output.Type != "File" {
		t.Errorf("The output type was '%s' when it should have been 'File'", output.Type)
	}
}

func TestOutputIdentifier(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	actual := output.Identifier("0-0")
	expected := "output-0-0"
	if actual != expected {
		t.Errorf("Identifier() returned %s instead of %s", actual, expected)
	}
}

func TestOutputStdout(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	actual := output.Stdout("0-0")
	expected := "logs/logs-stdout-output-0-0"
	if actual != expected {
		t.Errorf("StepOutput.Stdout() returned %s instead of %s", actual, expected)
	}
}

func TestOutputStderr(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	actual := output.Stderr("0-0")
	expected := "logs/logs-stderr-output-0-0"
	if actual != expected {
		t.Errorf("StepOuput.Stderr() returned %s instead of %s", actual, expected)
	}
}

func TestOutputLogPath(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	actual := output.LogPath("parent", "0-0")
	expected := "parent/logs/logs-condor-output-0-0"
	if actual != expected {
		t.Errorf("StepOutput.LogPath() returned %s instead of %s", actual, expected)
	}
}

func TestOutputArguments(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	actual := output.Arguments("testuser", "/irods/dest/")
	expected := "run --rm -a stdout -a stderr -v $(pwd):/de-app-work -w /de-app-work discoenv/porklock:test put --user testuser --source 'wc_out.txt' --destination '/irods/dest/' --config logs/irods-config"
	if actual != expected {
		t.Errorf("Arguments returned:\n\t%s\ninstead of:\n\t%s", actual, expected)
	}
}

func TestOutputSource(t *testing.T) {
	s := inittests(t)
	output := s.Steps[0].Config.Outputs[0]
	actual := output.Source()
	expected := "wc_out.txt"
	if actual != expected {
		t.Errorf("Source() returned %s instead of %s", actual, expected)
	}
	output.Name = "not-abs"
	output.Multiplicity = "collection"
	actual = output.Source()
	expected = "$(pwd)/not-abs/"
	if actual != expected {
		t.Errorf("Source() returned %s instead of %s", actual, expected)
	}
	output.Name = "/abs/path"
	output.Multiplicity = "collection"
	actual = output.Source()
	expected = "/abs/path/"
	if actual != expected {
		t.Errorf("Source() returned %s instead of %s", actual, expected)
	}
	output.Name = "/abs/path/"
	output.Multiplicity = "collection"
	actual = output.Source()
	expected = "/abs/path/"
	if actual != expected {
		t.Errorf("Source() returned %s instead of %s", actual, expected)
	}
	_inittests(t, false)
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
	if params.Order != 2 {
		t.Errorf("The param order was '%d' when it should have been '2'", params.Order)
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
	if params.Order != 1 {
		t.Errorf("The param order was '%d' when it should have been '1'", params.Order)
	}
}

func TestConfigParams1Value(t *testing.T) {
	s := inittests(t)
	params := s.Steps[0].Config.Params[1]
	if params.Value != "Acer-tree.txt" {
		t.Errorf("The param value was '%s' when it should have been 'Acer-tree.txt'", params.Value)
	}
}
