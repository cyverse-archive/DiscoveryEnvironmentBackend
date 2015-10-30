package main

import (
	"bytes"
	"configurate"
	"fmt"
	"io/ioutil"
	"logcabin"
	"model"
	"os"
	"reflect"
	"strconv"
	"strings"
	"testing"

	"github.com/fsouza/go-dockerclient"
)

var (
	s *model.Job
	l = logcabin.New()
)

func shouldrun() bool {
	if os.Getenv("DIND_PORT_2375_TCP_ADDR") != "" {
		return true
	}
	return false
}

func uri() string {
	addr := os.Getenv("DIND_PORT_2375_TCP_ADDR")
	port := os.Getenv("DIND_PORT_2375_TCP_PORT")
	return fmt.Sprintf("http://%s:%s", addr, port)
}

func JSONData() ([]byte, error) {
	f, err := os.Open("../test/test_runner.json")
	if err != nil {
		return nil, err
	}
	c, err := ioutil.ReadAll(f)
	if err != nil {
		return nil, err
	}
	return c, err
}

func _inittests(t *testing.T, memoize bool) *model.Job {
	if s == nil || !memoize {
		configurate.Init("../test/test_config.json")
		configurate.C.Set("irods.base", "/path/to/irodsbase")
		configurate.C.Set("irods.host", "hostname")
		configurate.C.Set("irods.port", "1247")
		configurate.C.Set("irods.user", "user")
		configurate.C.Set("irods.pass", "pass")
		configurate.C.Set("irods.zone", "test")
		configurate.C.Set("irods.resc", "")
		configurate.C.Set("condor.log_path", "/path/to/logs")
		configurate.C.Set("condor.porklock_tag", "test")
		configurate.C.Set("condor.filter_files", "foo,bar,baz,blippy")
		configurate.C.Set("condor.request_disk", "0")
		data, err := JSONData()
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		s, err = model.NewFromData(data)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
		err = configurate.Init("../test/test_config.yaml")
		if err != nil {
			logger.Fatal(err)
		}
	}
	return s
}

func inittests(t *testing.T) *model.Job {
	return _inittests(t, true)
}

func TestNewDocker(t *testing.T) {
	if !shouldrun() {
		return
	}
	_, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
}

func TestIsContainer(t *testing.T) {
	if !shouldrun() {
		return
	}
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	actual, err := dc.IsContainer("test_not_there")
	if err != nil {
		t.Error(err)
	}
	if actual {
		t.Error("IsContainer returned true instead of false")
	}
}

func TestPull(t *testing.T) {
	if !shouldrun() {
		return
	}
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = dc.Pull("alpine", "latest")
	if err != nil {
		t.Error(err)
	}
}

func TestCreateIsContainerAndNukeByName(t *testing.T) {
	if !shouldrun() {
		return
	}
	job := inittests(t)
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = dc.Pull("alpine", "latest")
	if err != nil {
		t.Error(err)
	}
	exists, err := dc.IsContainer(job.Steps[0].Component.Container.Name)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(job.Steps[0].Component.Container.Name)
	}
	container, opts, err := dc.CreateContainerFromStep(&job.Steps[0], job.InvocationID)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if container.ID == "" {
		t.Error("CreateContainerFromStep created a container with a blank ID")
	}
	if opts == nil {
		t.Error("CreatecontainerFromStep created a nil opts")
	}

	expected := job.Steps[0].Component.Container.MemoryLimit
	actual := strconv.FormatInt(opts.Config.Memory, 10)
	if actual != expected {
		t.Errorf("Config.Memory was %s instead of %s\n", actual, expected)
	}

	expected = job.Steps[0].Component.Container.CPUShares
	actual = strconv.FormatInt(opts.Config.CPUShares, 10)
	if actual != expected {
		t.Errorf("Config.CPUShares was %s instead of %s\n", actual, expected)
	}

	expected = job.Steps[0].Component.Container.EntryPoint
	actual = opts.Config.Entrypoint[0]
	if actual != expected {
		t.Errorf("Config.Entrypoint was %s instead of %s\n", actual, expected)
	}

	expected = job.Steps[0].Component.Container.NetworkMode
	actual = opts.HostConfig.NetworkMode
	if actual != expected {
		t.Errorf("HostConfig.NetworkMode was %s instead of %s\n", actual, expected)
	}

	expected = "alpine:latest"
	actual = opts.Config.Image
	if actual != expected {
		t.Errorf("Config.Image was %s instead of %s\n", actual, expected)
	}

	expected = "/work"
	actual = opts.Config.WorkingDir
	if actual != expected {
		t.Errorf("Config.WorkingDir was %s instead of %s\n", actual, expected)
	}

	found := false
	for _, e := range opts.Config.Env {
		if e == "food=banana" {
			found = true
		}
	}
	if !found {
		t.Error("Didn't find 'food=banana' in Config.Env.")
	}

	found = false
	for _, e := range opts.Config.Env {
		if e == "foo=bar" {
			found = true
		}
	}
	if !found {
		t.Error("Didn't find 'foo=bar' in Config.Env.")
	}

	expectedConfig := docker.LogConfig{Type: "none"}
	actualConfig := opts.HostConfig.LogConfig
	if !reflect.DeepEqual(actualConfig, expectedConfig) {
		t.Errorf("HostConfig.LogConfig was %#v instead of %#v\n", actualConfig, expectedConfig)
	}

	expectedList := []string{"This is a test"}
	actualList := opts.Config.Cmd
	if !reflect.DeepEqual(expectedList, actualList) {
		t.Errorf("Config.Cmd was:\n\t%#v\ninstead of:\n\t%#v\n", actualList, expectedList)
	}

	//TODO: Test Devices
	//TODO: Test VolumesFrom
	//TODO: Test Volumes

	exists, err = dc.IsContainer(job.Steps[0].Component.Container.Name)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(job.Steps[0].Component.Container.Name)
	}
}

func TestCreateDownloadContainer(t *testing.T) {
	if !shouldrun() {
		return
	}
	job := inittests(t)
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	image, err := configurate.C.String("porklock.image")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	tag, err := configurate.C.String("porklock.tag")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = dc.Pull(image, tag)
	if err != nil {
		t.Error(err)
	}
	cName := fmt.Sprintf("input-0-%s", job.InvocationID)
	exists, err := dc.IsContainer(cName)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(cName)
	}
	container, opts, err := dc.CreateDownloadContainer(job, &job.Steps[0].Config.Inputs[0], "0")
	if err != nil {
		t.Error(err)
		t.Fail()
	}

	if container.Name != cName {
		t.Errorf("container name was %s instead of %s", container.Name, cName)
	}

	expected := fmt.Sprintf("%s:%s", image, tag)
	actual := opts.Config.Image
	if actual != expected {
		t.Errorf("Image was %s instead of %s", actual, expected)
	}

	expected = "/de-app-work"
	actual = opts.Config.WorkingDir
	if actual != expected {
		t.Errorf("WorkingDir was %s instead of %s", actual, expected)
	}

	expectedList := job.Steps[0].Config.Inputs[0].Arguments(job.Submitter, job.FileMetadata)
	actualList := opts.Config.Cmd
	if !reflect.DeepEqual(actualList, expectedList) {
		t.Errorf("Cmd was:\n%#v\ninstead of:\n%#v\n", actualList, expectedList)
	}
	wd, err := os.Getwd()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	expectedMount := docker.Mount{
		Source:      wd,
		Destination: "/de-app-work",
		RW:          true,
	}
	if len(opts.Config.Mounts) != 1 {
		t.Errorf("Number of mounts was %d instead of 1", len(opts.Config.Mounts))
	} else {
		actualMount := opts.Config.Mounts[0]
		if !reflect.DeepEqual(actualMount, expectedMount) {
			t.Errorf("Mount was:\n%#v\ninstead of:\n%#v", actualMount, expectedMount)
		}
	}
	if _, ok := opts.Config.Labels[model.DockerLabelKey]; !ok {
		t.Error("Label was not set")
	} else {
		actual = opts.Config.Labels[model.DockerLabelKey]
		expected = job.InvocationID
		if actual != expected {
			t.Errorf("The label was set to %s instead of %s", actual, expected)
		}
	}

	expectedLogConfig := docker.LogConfig{Type: "none"}
	actualLogConfig := opts.HostConfig.LogConfig
	if !reflect.DeepEqual(actualLogConfig, expectedLogConfig) {
		t.Errorf("LogConfig was:\n%#v\ninstead of:\n%#v\n", actualLogConfig, expectedLogConfig)
	}
}

func TestCreateUploadContainer(t *testing.T) {
	if !shouldrun() {
		return
	}
	job := inittests(t)
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	image, err := configurate.C.String("porklock.image")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	tag, err := configurate.C.String("porklock.tag")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	containerName := fmt.Sprintf("output-%s", job.InvocationID)
	exists, err := dc.IsContainer(containerName)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(containerName)
	}
	container, opts, err := dc.CreateUploadContainer(job)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if container.Name != containerName {
		t.Errorf("container name was %s instead of %s", container.Name, containerName)
	}

	expected := fmt.Sprintf("%s:%s", image, tag)
	actual := opts.Config.Image
	if actual != expected {
		t.Errorf("Image was %s instead of %s", actual, expected)
	}

	expected = "/de-app-work"
	actual = opts.Config.WorkingDir
	if actual != expected {
		t.Errorf("WorkingDir was %s instead of %s", actual, expected)
	}

	expectedList := job.FinalOutputArguments()
	actualList := opts.Config.Cmd
	if !reflect.DeepEqual(actualList, expectedList) {
		t.Errorf("Cmd was:\n%#v\ninstead of:\n%#v\n", actualList, expectedList)
	}
	wd, err := os.Getwd()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	expectedMount := docker.Mount{
		Source:      wd,
		Destination: "/de-app-work",
		RW:          true,
	}
	if len(opts.Config.Mounts) != 1 {
		t.Errorf("Number of mounts was %d instead of 1", len(opts.Config.Mounts))
	} else {
		actualMount := opts.Config.Mounts[0]
		if !reflect.DeepEqual(actualMount, expectedMount) {
			t.Errorf("Mount was:\n%#v\ninstead of:\n%#v", actualMount, expectedMount)
		}
	}
	if _, ok := opts.Config.Labels[model.DockerLabelKey]; !ok {
		t.Error("Label was not set")
	} else {
		actual = opts.Config.Labels[model.DockerLabelKey]
		expected = job.InvocationID
		if actual != expected {
			t.Errorf("The label was set to %s instead of %s", actual, expected)
		}
	}

	expectedLogConfig := docker.LogConfig{Type: "none"}
	actualLogConfig := opts.HostConfig.LogConfig
	if !reflect.DeepEqual(actualLogConfig, expectedLogConfig) {
		t.Errorf("LogConfig was:\n%#v\ninstead of:\n%#v\n", actualLogConfig, expectedLogConfig)
	}
}

func TestAttach(t *testing.T) {
	if !shouldrun() {
		return
	}
	job := inittests(t)
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = dc.Pull("alpine", "latest")
	if err != nil {
		t.Error(err)
	}
	exists, err := dc.IsContainer(job.Steps[0].Component.Container.Name)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(job.Steps[0].Component.Container.Name)
	}
	container, _, err := dc.CreateContainerFromStep(&job.Steps[0], job.InvocationID)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	stdout := bytes.NewBufferString("")
	stderr := bytes.NewBufferString("")
	success := make(chan struct{})
	go func() {
		err = dc.Attach(container, stdout, stderr, success)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
	}()
	<-success

	exists, err = dc.IsContainer(job.Steps[0].Component.Container.Name)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(job.Steps[0].Component.Container.Name)
	}
}

func TestRunStep(t *testing.T) {
	if !shouldrun() {
		return
	}
	job := inittests(t)
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = dc.Pull("alpine", "latest")
	if err != nil {
		t.Error(err)
	}
	exists, err := dc.IsContainer(job.Steps[0].Component.Container.Name)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(job.Steps[0].Component.Container.Name)
	}
	if _, err := os.Stat("logs"); os.IsNotExist(err) {
		err = os.MkdirAll("logs", 0755)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
	}
	exitCode, err := dc.RunStep(&job.Steps[0], job.InvocationID, 0)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if exitCode != 0 {
		t.Errorf("RunStep's exit code was %d instead of 0\n", exitCode)
	}
	if _, err := os.Stat(job.Steps[0].Stdout("0")); os.IsNotExist(err) {
		t.Error(err)
	}
	if _, err := os.Stat(job.Steps[0].Stderr("0")); os.IsNotExist(err) {
		t.Error(err)
	}
	expected := "This is a test"
	actualBytes, err := ioutil.ReadFile(job.Steps[0].Stdout("0"))
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	actual := strings.TrimSpace(string(actualBytes))
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("stdout contained '%s' instead of '%s'\n", string(actual), string(expected))
	}
	err = os.RemoveAll("logs")
	if err != nil {
		t.Error(err)
	}
	exists, err = dc.IsContainer(job.Steps[0].Component.Container.Name)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(job.Steps[0].Component.Container.Name)
	}
}

func TestDownloadInputs(t *testing.T) {
	if !shouldrun() {
		return
	}
	job := inittests(t)
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	image, err := configurate.C.String("porklock.image")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	tag, err := configurate.C.String("porklock.tag")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = dc.Pull(image, tag)
	if err != nil {
		t.Error(err)
	}
	cName := fmt.Sprintf("input-0-%s", job.InvocationID)
	exists, err := dc.IsContainer(cName)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(cName)
	}
	if _, err := os.Stat("logs"); os.IsNotExist(err) {
		err = os.MkdirAll("logs", 0755)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
	}
	exitCode, err := dc.DownloadInputs(job, &job.Steps[0].Config.Inputs[0], 0)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if exitCode != 0 {
		t.Errorf("DownloadInputs's exit code was %d instead of 0\n", exitCode)
	}
	if _, err := os.Stat(job.Steps[0].Config.Inputs[0].Stdout("0")); os.IsNotExist(err) {
		t.Error(err)
	}
	if _, err := os.Stat(job.Steps[0].Config.Inputs[0].Stderr("0")); os.IsNotExist(err) {
		t.Error(err)
	}
	expected := strings.Join(
		job.Steps[0].Config.Inputs[0].Arguments(job.Submitter, job.FileMetadata),
		" ",
	)
	actualBytes, err := ioutil.ReadFile(job.Steps[0].Config.Inputs[0].Stdout("0"))
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	actual := strings.TrimSpace(string(actualBytes))
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("stdout contained '%s' instead of '%s'\n", string(actual), string(expected))
	}
	err = os.RemoveAll("logs")
	if err != nil {
		t.Error(err)
	}
	exists, err = dc.IsContainer(cName)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(cName)
	}
}

func TestUploadOutputs(t *testing.T) {
	if !shouldrun() {
		return
	}
	job := inittests(t)
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	image, err := configurate.C.String("porklock.image")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	tag, err := configurate.C.String("porklock.tag")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = dc.Pull(image, tag)
	if err != nil {
		t.Error(err)
	}
	cName := fmt.Sprintf("output-%s", job.InvocationID)
	exists, err := dc.IsContainer(cName)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(cName)
	}
	if _, err := os.Stat("logs"); os.IsNotExist(err) {
		err = os.MkdirAll("logs", 0755)
		if err != nil {
			t.Error(err)
			t.Fail()
		}
	}
	exitCode, err := dc.UploadOutputs(job)
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if exitCode != 0 {
		t.Errorf("UploadOutputs exit code was %d instead of 0\n", exitCode)
	}
	if _, err := os.Stat("logs/logs-stdout-output"); os.IsNotExist(err) {
		t.Error(err)
	}
	if _, err := os.Stat("logs/logs-stderr-output"); os.IsNotExist(err) {
		t.Error(err)
	}
	expected := strings.Join(
		job.FinalOutputArguments(),
		" ",
	)
	actualBytes, err := ioutil.ReadFile("logs/logs-stdout-output")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	actual := strings.TrimSpace(string(actualBytes))
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("stdout contained '%s' instead of '%s'\n", string(actual), string(expected))
	}
	err = os.RemoveAll("logs")
	if err != nil {
		t.Error(err)
	}
	exists, err = dc.IsContainer(cName)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(cName)
	}
}

func TestCreateDataContainer(t *testing.T) {
	if !shouldrun() {
		return
	}
	job := inittests(t)
	dc, err := NewDocker(uri())
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	image, err := configurate.C.String("porklock.image")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	tag, err := configurate.C.String("porklock.tag")
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	err = dc.Pull(image, tag)
	if err != nil {
		t.Error(err)
	}
	vf := &model.VolumesFrom{
		Name:          "discoenv/echo",
		NamePrefix:    "echo-test",
		Tag:           "latest",
		URL:           "https://hub.docker.com/r/discoenv/echo/",
		ReadOnly:      false,
		HostPath:      "/tmp",
		ContainerPath: "/test",
	}
	cName := fmt.Sprintf("%s-%s", vf.NamePrefix, job.InvocationID)
	exists, err := dc.IsContainer(cName)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(cName)
	}
	container, opts, err := dc.CreateDataContainer(vf, job.InvocationID)
	if err != nil {
		t.Error(err)
	}
	if container == nil {
		t.Error("container was nil")
	}
	if opts == nil {
		t.Error("opts was nil")
	}
	exists, err = dc.IsContainer(cName)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(cName)
	}

}
