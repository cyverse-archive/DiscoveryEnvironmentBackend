package model

import "testing"

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
