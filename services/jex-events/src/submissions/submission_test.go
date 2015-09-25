package submissions

import (
	"bytes"
	"fmt"
	"path"
	"strings"
	"testing"
	"time"
)

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
	if s.Username != "test_this_is_a_test" {
		t.Errorf("The username was '%s' instead of 'test_this_is_a_test'", s.Username)
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

func TestDataContainers(t *testing.T) {
	s := inittests(t)
	dc := s.DataContainers()
	dclen := len(dc)
	if dclen != 2 {
		t.Errorf("The number of data containers was '%d' instead of 2", dclen)
	}

	vfs := dc[0]
	if vfs.Name != "vf-name1" {
		t.Errorf("The VolumesFrom name was '%s' when it should have been 'vf-name1'", vfs.Name)
	}
	if vfs.NamePrefix != "vf-prefix1" {
		t.Errorf("The VolumesFrom prefix was '%s' when it should have been 'vf-prefix1'", vfs.NamePrefix)
	}
	if vfs.Tag != "vf-tag1" {
		t.Errorf("The VolumesFrom tag was '%s' when it should have been 'vf-tag1'", vfs.Tag)
	}
	if vfs.URL != "vf-url1" {
		t.Errorf("The VolumesFrom url was '%s' when it should have been 'vf-url1'", vfs.URL)
	}
	if vfs.HostPath != "/host/path1" {
		t.Errorf("The VolumesFrom host path was '%s' when it should have been '/host/path1'", vfs.HostPath)
	}
	if vfs.ContainerPath != "/container/path1" {
		t.Errorf("The VolumesFrom container path was '%s' when it should have been '/container/path1'", vfs.ContainerPath)
	}
	if !vfs.ReadOnly {
		t.Error("The VolumesFrom read-only field was false when it should have been true.")
	}

	vfs = dc[1]
	if vfs.Name != "vf-name2" {
		t.Errorf("The VolumesFrom name was '%s' when it should have been 'vf-name2'", vfs.Name)
	}
	if vfs.NamePrefix != "vf-prefix2" {
		t.Errorf("The VolumesFrom prefix was '%s' when it should have been 'vf-prefix2'", vfs.NamePrefix)
	}
	if vfs.Tag != "vf-tag2" {
		t.Errorf("The VolumesFrom tag was '%s' when it should have been 'vf-tag2'", vfs.Tag)
	}
	if vfs.URL != "vf-url2" {
		t.Errorf("The VolumesFrom url was '%s' when it should have been 'vf-url2'", vfs.URL)
	}
	if vfs.HostPath != "/host/path2" {
		t.Errorf("The VolumesFrom host path was '%s' when it should have been '/host/path2'", vfs.HostPath)
	}
	if vfs.ContainerPath != "/container/path2" {
		t.Errorf("The VolumesFrom container path was '%s' when it should have been '/container/path2'", vfs.ContainerPath)
	}
	if !vfs.ReadOnly {
		t.Error("The VolumesFrom read-only field was false when it should have been true.")
	}
}

func TestContainerImages(t *testing.T) {
	s := inittests(t)
	ci := s.ContainerImages()
	actuallen := len(ci)
	expectedlen := 1
	if actuallen != expectedlen {
		t.Errorf("ContainerImages() return %d ContainerImages instead of %d", actuallen, expectedlen)
	}
	actual := ci[0].ID
	expected := "fc210a84-f7cd-4067-939c-a68ec3e3bd2b"
	if actual != expected {
		t.Errorf("ID was %s instead of %s", actual, expected)
	}
	actual = ci[0].Name
	expected = "gims.iplantcollaborative.org:5000/backwards-compat"
	if actual != expected {
		t.Errorf("Name was %s instead of %s", actual, expected)
	}
	actual = ci[0].Tag
	expected = "latest"
	if actual != expected {
		t.Errorf("Tag was %s instead of %s", actual, expected)
	}
	actual = ci[0].URL
	expected = "https://registry.hub.docker.com/u/discoenv/backwards-compat"
	if actual != expected {
		t.Errorf("URL was %s instead of %s", actual, expected)
	}
}

func TestFileMetadata(t *testing.T) {
	s := inittests(t)
	fm := s.FileMetadata
	actual := fm[0].Attribute
	expected := "attr1"
	if actual != expected {
		t.Errorf("Attribute was %s instead of %s", actual, expected)
	}
	actual = fm[0].Value
	expected = "value1"
	if actual != expected {
		t.Errorf("Value was %s instead of %s", actual, expected)
	}
	actual = fm[0].Unit
	expected = "unit1"
	if actual != expected {
		t.Errorf("Unit was %s instead of %s", actual, expected)
	}
	actual = fm[1].Attribute
	expected = "attr2"
	if actual != expected {
		t.Errorf("Attribute was %s instead of %s", actual, expected)
	}
	actual = fm[1].Value
	expected = "value2"
	if actual != expected {
		t.Errorf("Value was %s instead of %s", actual, expected)
	}
	actual = fm[1].Unit
	expected = "unit2"
	if actual != expected {
		t.Errorf("Unit was %s instead of %s", actual, expected)
	}
}

func TestFileMetadataArgument(t *testing.T) {
	s := inittests(t)
	fm := s.FileMetadata
	actual := fm[0].Argument()
	expected := "-m 'attr1,value1,unit1'"
	if actual != expected {
		t.Errorf("Argument() returned %s instead of %s", actual, expected)
	}
	actual = fm[1].Argument()
	expected = "-m 'attr2,value2,unit2'"
	if actual != expected {
		t.Errorf("Argument() returned %s instead of %s", actual, expected)
	}
}

func TestSubmissionFileMetadataArguments(t *testing.T) {
	s := inittests(t)
	actual := MetadataArgs(s.FileMetadata).FileMetadataArguments()
	expected := "-m 'attr1,value1,unit1' -m 'attr2,value2,unit2' -m 'ipc-analysis-id,c7f05682-23c8-4182-b9a2-e09650a5f49b,UUID' -m 'ipc-execution-id,07b04ce2-7757-4b21-9e15-0b4c2f44be26,UUID'"
	if actual != expected {
		t.Errorf("FileMetadataArguments() returned %s instead of %s", actual, expected)
	}
}

func TestInputs(t *testing.T) {
	s := inittests(t)
	inputs := s.Inputs()
	actual := len(inputs)
	expected := 1
	if actual != expected {
		t.Errorf("Number of inputs was %d instead of %d", actual, expected)
	}
}

func TestOutputs(t *testing.T) {
	s := inittests(t)
	outputs := s.Outputs()
	actual := len(outputs)
	expected := 2
	if actual != expected {
		t.Errorf("Number of outputs was %d instead of %d", actual, expected)
	}
}

func TestExcludeArguments(t *testing.T) {
	s := inittests(t)
	actual := s.ExcludeArguments()
	expected := "--exclude foo,bar,baz,blippy"
	if actual != expected {
		t.Errorf("ExcludeArguments() returned:\n\t%sinstead of:\n\t%s", actual, expected)
	}
	s.Steps[0].Config.Inputs[0].Retain = false
	actual = s.ExcludeArguments()
	expected = "--exclude Acer-tree.txt,foo,bar,baz,blippy"
	if actual != expected {
		t.Errorf("ExcludeArguments() returned:\n\t%sinstead of:\n\t%s", actual, expected)
	}
	s.Steps[0].Config.Outputs[1].Retain = false
	actual = s.ExcludeArguments()
	expected = "--exclude Acer-tree.txt,$(pwd)/logs/,foo,bar,baz,blippy"
	if actual != expected {
		t.Errorf("ExcludeArguments() returned:\n\t%sinstead of:\n\t%s", actual, expected)
	}
	s.ArchiveLogs = false
	actual = s.ExcludeArguments()
	expected = "--exclude Acer-tree.txt,$(pwd)/logs/,foo,bar,baz,blippy,logs"
	if actual != expected {
		t.Errorf("ExcludeArguments() returned:\n\t%sinstead of:\n\t%s", actual, expected)
	}
	_inittests(t, false)
}

func TestAddRequiredMetadata(t *testing.T) {
	s := inittests(t)
	found := false
	var a FileMetadata
	for _, md := range s.FileMetadata {
		if md.Attribute == "ipc-analysis-id" {
			found = true
			a = md
		}
	}
	if !found {
		t.Errorf("ipc-analysis-id was not in the file metadata before AddRequiredMetadata() was called")
	}
	if found {
		if a.Value != s.AppID {
			t.Errorf("Value was set to %s instead of %s", a.Value, s.AppID)
		}
		if a.Unit != "UUID" {
			t.Errorf("Unit was set to %s instead of %s", a.Unit, "UUID")
		}
	}
	found = false
	var e FileMetadata
	for _, md := range s.FileMetadata {
		if md.Attribute == "ipc-execution-id" {
			found = true
			e = md
		}
	}
	if !found {
		t.Errorf("ipc-execution-id was not in the file metadata before AddRequiredMetadata() was called")
	}
	if found {
		if e.Value != s.UUID {
			t.Errorf("Value was set to %s instead of %s", e.Value, s.UUID)
		}
		if e.Unit != "UUID" {
			t.Errorf("Unit was set to %s instead of %s", e.Unit, "UUID")
		}
	}
	_inittests(t, false)
}

func TestFinalOutputArguments(t *testing.T) {
	s := inittests(t)
	s.AddRequiredMetadata()
	actual := s.FinalOutputArguments()
	outputdir := s.OutputDirectory()
	expected := fmt.Sprintf("run --rm -v $(pwd):/de-app-work -w /de-app-work discoenv/porklock:test put --user test_this_is_a_test --config irods-config --destination '%s' -m 'attr1,value1,unit1' -m 'attr2,value2,unit2' -m 'ipc-analysis-id,c7f05682-23c8-4182-b9a2-e09650a5f49b,UUID' -m 'ipc-execution-id,07b04ce2-7757-4b21-9e15-0b4c2f44be26,UUID' --exclude foo,bar,baz,blippy", outputdir)
	if actual != expected {
		t.Errorf("FinalOutputArguments() returned:\n\t%s\ninstead of:\n\t%s", actual, expected)
	}
	s.SkipParentMetadata = true
	actual = s.FinalOutputArguments()
	expected = fmt.Sprintf("run --rm -v $(pwd):/de-app-work -w /de-app-work discoenv/porklock:test put --user test_this_is_a_test --config irods-config --destination '%s' -m 'attr1,value1,unit1' -m 'attr2,value2,unit2' -m 'ipc-analysis-id,c7f05682-23c8-4182-b9a2-e09650a5f49b,UUID' -m 'ipc-execution-id,07b04ce2-7757-4b21-9e15-0b4c2f44be26,UUID' --exclude foo,bar,baz,blippy --skip-parent-meta", outputdir)
	if actual != expected {
		t.Errorf("FinalOutputArguments() returned:\n\t%s\ninstead of:\n\t%s", actual, expected)
	}
	_inittests(t, false)
}

func TestExtractJobID(t *testing.T) {
	testData := []byte(`1000 job(s) submitted to cluster (100000000.0000).`)
	actual := extractJobID(testData)
	expected := []byte("100000000.0000")
	if !bytes.Equal(actual, expected) {
		t.Errorf("extractJobID found %s instead of %s", actual, expected)
	}

	testData = []byte(`asdfadsfadsfadsfa1000 job(s) submitted to cluster (100000000.0000)asdfadsfadsfasdfadsfadsfadsfadsfadsf`)
	actual = extractJobID(testData)
	expected = []byte("100000000.0000")
	if !bytes.Equal(actual, expected) {
		t.Errorf("extractJobID found %s instead of %s", actual, expected)
	}

	testData = []byte(`asdfadsfadsfadsfa
adsfadsfadsfadsfadsfasdfadsfadsfadsfadsfdsa1000 job(s) submitted to cluster (100000000)asdfadsfadsfasdfadsfadsfadsfadsfadsf
asdfadsfasdfadsfdsfsdsfdsafds`)
	actual = extractJobID(testData)
	expected = []byte("100000000")
	if !bytes.Equal(actual, expected) {
		t.Errorf("extractJobID found %s instead of %s", actual, expected)
	}

}
