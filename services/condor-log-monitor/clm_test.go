package main

import (
	"os"
	"testing"
)

func TestInodeFromPath(t *testing.T) {
	path := "test_config.json"
	inode, err := InodeFromPath(path)
	if err != nil {
		t.Error(err)
	}
	if inode <= 0 {
		t.Errorf("inode set to %d", inode)
	}
}

func TestInodeFromFile(t *testing.T) {
	openFile, err := os.Open("test_config.json")
	if err != nil {
		t.Error(err)
	}
	inode, err := InodeFromFile(openFile)
	if err != nil {
		t.Error(err)
	}
	if inode <= 0 {
		t.Errorf("inode set to %d", inode)
	}
}

func TestNewTombstoneFromPath(t *testing.T) {
	path := "test_events.txt"
	tombstone, err := NewTombstoneFromPath(path)
	if err != nil {
		t.Error(err)
	}
	if tombstone == nil {
		t.Error("tombstone is nil")
	}

	if tombstone.CurrentPos < 0 {
		t.Errorf("CurrentPos is set to %d", tombstone.CurrentPos)
	}

	if tombstone.Date.IsZero() {
		t.Error("Date has not been set")
	}

	if tombstone.LogLastMod.IsZero() {
		t.Error("LogLastMod has not been set")
	}

	if tombstone.Inode == 0 {
		t.Error("Inode was set to zero")
	}
}

func TestNewTombstoneFromFile(t *testing.T) {
	openFile, err := os.Open("test_events.txt")
	if err != nil {
		t.Error(err)
	}
	tombstone, err := NewTombstoneFromFile(openFile)
	if err != nil {
		t.Error(err)
	}
	if tombstone == nil {
		t.Error("tombstone is nil")
	}

	if tombstone.CurrentPos < 0 {
		t.Errorf("CurrentPos is set to %d", tombstone.CurrentPos)
	}

	if tombstone.Date.IsZero() {
		t.Error("Date has not been set")
	}

	if tombstone.LogLastMod.IsZero() {
		t.Error("LogLastMod has not been set")
	}

	if tombstone.Inode == 0 {
		t.Error("Inode was set to zero")
	}
}

func TestWriteToFile(t *testing.T) {
	path := "test_events.txt"
	tombstone, err := NewTombstoneFromPath(path)
	if err != nil {
		t.Error(err)
	}
	if tombstone == nil {
		t.Error("tombstone is nil")
	}
	err = tombstone.WriteToFile()
	if err != nil {
		t.Error(err)
	}
	of, err := os.Open(TombstonePath)
	if err != nil {
		t.Error(err)
	}
	err = of.Close()
	if err != nil {
		t.Error(err)
	}
	err = os.Remove(TombstonePath)
	if err != nil {
		t.Error(err)
	}
}

func TestReadTombstone(t *testing.T) {
	path := "test_events.txt"
	t1, err := NewTombstoneFromPath(path)
	if err != nil {
		t.Error(err)
	}
	if t1 == nil {
		t.Error("tombstone is nil")
	}
	err = t1.WriteToFile()
	if err != nil {
		t.Error(err)
	}
	tombstone, err := ReadTombstone()
	if err != nil {
		t.Error(err)
	}
	if tombstone == nil {
		t.Error("tombstone is nil")
	}

	if tombstone.CurrentPos < 0 {
		t.Errorf("CurrentPos is set to %d", tombstone.CurrentPos)
	}

	if tombstone.Date.IsZero() {
		t.Error("Date has not been set")
	}

	if tombstone.LogLastMod.IsZero() {
		t.Error("LogLastMod has not been set")
	}

	if tombstone.Inode == 0 {
		t.Error("Inode was set to zero")
	}
	err = os.Remove(TombstonePath)
	if err != nil {
		t.Error(err)
	}
}
