package submissions

import "testing"

func TestGenerateCondorSubmit(t *testing.T) {
	s := inittests(t)
	actual, err := GenerateCondorSubmit(s)
	if err != nil {
		t.Error(err)
	}
	expected := `
universe = vanilla
executable = /bin/bash
rank = mips
arguments = "iplant.sh"
output = script-output.log
error = script-error.log
request_disk = 0
+IpcUuid = "07b04ce2-7757-4b21-9e15-0b4c2f44be26""
+IpcJobId = "generated_script"
+IpcUsername = "wregglej_this_is_a_test"
concurrency_limits = "wregglej_this_is_a_test"
+IpcExe = "wc_wrapper.sh"
+IpcExePath = "/usr/local3/bin/wc_tool-1.00"
should_transfer_files = YES
transfer_input_files = iplant.sh,irods-config.iplant.cmd
transfer_output_files = logs/de-transfer-trigger.log,logs/output-last-stdout,logs/output-last-stderr
when_to_transfer_output = ON_EXIT_OR_EVICT
notification = NEVER
queue
`
	if actual != expected {
		t.Errorf("GenerateCondorSubmit() returned:\n\n%s\n\ninstead of:\n\n%s", actual, expected)
	}
	s.Group = "foo"
	actual, err = GenerateCondorSubmit(s)
	if err != nil {
		t.Error(err)
	}
	expected = `
universe = vanilla
executable = /bin/bash
rank = mips
arguments = "iplant.sh"
output = script-output.log
error = script-error.log
request_disk = 0
+IpcUuid = "07b04ce2-7757-4b21-9e15-0b4c2f44be26""
+IpcJobId = "generated_script"
+IpcUsername = "wregglej_this_is_a_test"
+AccountingGroup = "foo.wregglej_this_is_a_test"
concurrency_limits = "wregglej_this_is_a_test"
+IpcExe = "wc_wrapper.sh"
+IpcExePath = "/usr/local3/bin/wc_tool-1.00"
should_transfer_files = YES
transfer_input_files = iplant.sh,irods-config.iplant.cmd
transfer_output_files = logs/de-transfer-trigger.log,logs/output-last-stdout,logs/output-last-stderr
when_to_transfer_output = ON_EXIT_OR_EVICT
notification = NEVER
queue
`
	if actual != expected {
		t.Errorf("GenerateCondorSubmit() returned:\n\n%s\n\ninstead of:\n\n%s", actual, expected)
	}

}
