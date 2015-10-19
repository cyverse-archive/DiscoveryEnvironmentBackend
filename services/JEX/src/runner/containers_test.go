package main

import (
	"configurate"
	"fmt"
	"io/ioutil"
	"logcabin"
	"model"
	"os"
	"reflect"
	"strconv"
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
		configurate.C.Set("condor.run_on_nfs", true)
		configurate.C.Set("condor.nfs_base", "/path/to/base")
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

	exists, err = dc.IsContainer(job.Steps[0].Component.Container.Name)
	if err != nil {
		t.Error(err)
	}
	if exists {
		dc.NukeContainerByName(job.Steps[0].Component.Container.Name)
	}
}
