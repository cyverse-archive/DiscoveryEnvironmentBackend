package configurate

import (
	"bytes"
	"log"
	"testing"
)

func configurator() (*Configuration, error) {
	path := "test_config.json"
	var logbuf bytes.Buffer
	logger := log.New(&logbuf, "", log.Lshortfile)
	return New(path, logger)
}

func TestNew(t *testing.T) {
	cfg, err := configurator()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if cfg == nil {
		t.Errorf("configurate.New() returned nil")
	}
}

func TestValid(t *testing.T) {
	cfg, err := configurator()
	if err != nil {
		t.Error(err)
		t.Fail()
	}
	if !cfg.Valid() {
		t.Errorf("configurate.Valid() return false")
	}
}
