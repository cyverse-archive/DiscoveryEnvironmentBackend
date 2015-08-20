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

// TestSetInvocationID tests the function that parses out the invocation id from
// event text.
func TestSetInvocationID(t *testing.T) {
	text := `
028 (4165.000.000) 04/27 13:55:45 Job ad information event triggered.
TotalLocalUsage = "Usr 0 00:00:00, Sys 0 00:00:00"
Proc = 0
EventTime = "2015-04-27T13:55:45"
TriggerEventTypeName = "ULOG_JOB_TERMINATED"
TotalRemoteUsage = "Usr 0 00:00:08, Sys 0 00:00:00"
ReturnValue = 0
TotalReceivedBytes = 801816.0
TriggerEventTypeNumber = 5
IpcUuid = "995f0ee0-8a8d-44e3-a3bb-a2f58210c65e"
RunRemoteUsage = "Usr 0 00:00:08, Sys 0 00:00:00"
RunLocalUsage = "Usr 0 00:00:00, Sys 0 00:00:00"
SentBytes = 93244.0
MyType = "JobTerminatedEvent"
Cluster = 4165
Subproc = 0
TotalSentBytes = 93244.0
EventTypeNumber = 28
CurrentTime = time()
TerminatedNormally = true
ReceivedBytes = 801816.0
...`
	e := Event{
		Event: text,
	}
	e.setInvocationID()
	if e.InvocationID != "995f0ee0-8a8d-44e3-a3bb-a2f58210c65e" {
		fmt.Printf("InvocationID is set to %s and not %s", e.InvocationID, "995f0ee0-8a8d-44e3-a3bb-a2f58210c65e")
	}
}
