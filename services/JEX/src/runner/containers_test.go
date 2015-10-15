package main

import (
	"fmt"
	"os"
	"testing"
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
