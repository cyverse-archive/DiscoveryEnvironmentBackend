package submissions

import (
	"bytes"
	"text/template"
)

// GenerateCondorSubmit returns a string (or error) containing the contents
// of what should go into an HTCondor submission file.
func GenerateCondorSubmit(submission *Submission) (string, error) {
	tmpl := `
universe = vanilla
executable = /bin/bash
rank = mips
arguments = "iplant.sh"
output = script-output.log
error = script-error.log
request_disk = {{.RequestDisk}}
+IpcUuid = "{{.UUID}}""
+IpcJobId = "generated_script"
+IpcUsername = "{{.Username}}"{{if .Group}}
+AccountingGroup = "{{.Group}}.{{.Username}}"{{end}}
concurrency_limits = "{{.Username}}"
{{with $x := index .Steps 0}}+IpcExe = "{{$x.Component.Name}}"{{end}}
{{with $x := index .Steps 0}}+IpcExePath = "{{$x.Component.Location}}"{{end}}
should_transfer_files = YES
transfer_input_files = iplant.sh,irods-config.iplant.cmd
transfer_output_files = logs/de-transfer-trigger.log,logs/output-last-stdout,logs/output-last-stderr
when_to_transfer_output = ON_EXIT_OR_EVICT
notification = NEVER
queue
`
	t, err := template.New("condor_submit").Parse(tmpl)
	if err != nil {
		return "", err
	}
	var buffer bytes.Buffer
	err = t.Execute(&buffer, submission)
	if err != nil {
		return "", err
	}
	return buffer.String(), err
}
