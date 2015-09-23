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

type scriptable struct {
	Submission
	DC []VolumesFrom
}

// GenerateIplantScript returns a string (or error) containing the contents
// of what should go into the iplant.sh script that is executed by the HTCondor
// job out on the cluster.
func GenerateIplantScript(submission *Submission) (string, error) {
	s := scriptable{
		Submission: *submission,
		DC:         submission.DataContainers(),
	}

	tmpl := `
#!/bin/bash
set -x
readonly IPLANT_USER={{.Username}}
export IPLANT_USER
readonly IPLANT_EXECUTION_ID={{.UUID}}
export IPLANT_EXECUTION_ID
export SCRIPT_LOCATION=${BASH_SOURCE}
EXITSTATUS=0
if [ -e /data2 ]; then ls /data2; fi
mkdir -p logs
if [ ! "$?" -eq "0"]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi
ls -al > logs/de-transfer-trigger.log
if [ ! "$?" -eq "0"]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi
if [ -e "iplant.sh" ]; then
	mv iplant.sh logs/
fi
if [ -e "iplant.cmd" ]; then
	mv iplant.cmd logs/
fi
{{range .DC}}docker pull {{.Name}}:{{.Tag}}
{{end}}
`
	t, err := template.New("iplant_script").Parse(tmpl)
	if err != nil {
		return "", err
	}
	var buffer bytes.Buffer
	err = t.Execute(&buffer, s)
	if err != nil {
		return "", err
	}
	return buffer.String(), err
}
