package main

import (
	"fmt"
	"testing"
)

// TestExtractCondorID test the ExtractCondorID function
func TestExtractCondorID(t *testing.T) {
	e := Event{
		ID: `(100.0.0)`,
	}
	//input :=
	e.setCondorID()
	if e.CondorID != "100" {
		t.Error("The extracted condor ID was not '100'")
	}
	//input2 :=
	e2 := Event{
		ID: `100.0.0`,
	}
	e2.setCondorID()
	if e2.CondorID != "" {
		t.Error("The extracted condor ID was not blank")
	}
}

// TestSetExitCode tests the function that parses out the return value
// from event text.
func TestSetExitCode(t *testing.T) {
	text := `
                              event_text
-----------------------------------------------------------------------
 005 (222.000.000) 11/05 14:18:27 Job terminated.                     +
         (1) Normal termination (return value 0)                      +
                 Usr 0 00:00:06, Sys 0 00:00:00  -  Run Remote Usage  +
                 Usr 0 00:00:00, Sys 0 00:00:00  -  Run Local Usage   +
                 Usr 0 00:00:06, Sys 0 00:00:00  -  Total Remote Usage+
                 Usr 0 00:00:00, Sys 0 00:00:00  -  Total Local Usage +
         123091  -  Run Bytes Sent By Job                             +
         801816  -  Run Bytes Received By Job                         +
         123091  -  Total Bytes Sent By Job                           +
         801816  -  Total Bytes Received By Job                       +
         Partitionable Resources :    Usage  Request Allocated        +
            Cpus                 :                 1         1        +
            Disk (KB)            :     1019     1024   2825331        +
            Memory (MB)          :        3        1      1024        +
 ...                                                                  +

(1 row)
`
	e := Event{
		Event: text,
	}
	e.setExitCode()
	if e.ExitCode != 0 {
		fmt.Printf("ExitCode is set to %d and not 0", e.ExitCode)
	}
}
