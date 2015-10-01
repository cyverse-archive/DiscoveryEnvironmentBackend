package submissions

import (
	"bytes"
	"clients"
	"configurate"
	"errors"
	"fmt"
	"io/ioutil"
	"logcabin"
	"model"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"text/template"
)

var (
	logger = logcabin.New()
)

// GenerateCondorSubmit returns a string (or error) containing the contents
// of what should go into an HTCondor submission file.
func GenerateCondorSubmit(submission *model.Job) (string, error) {
	tmpl := `universe = vanilla
executable = /bin/bash
rank = mips
arguments = "iplant.sh"
output = script-output.log
error = script-error.log
log = condor.log
request_disk = {{.RequestDisk}}
+IpcUuid = "{{.InvocationID}}"
+IpcJobId = "generated_script"
+IpcUsername = "{{.Submitter}}"{{if .Group}}
+AccountingGroup = "{{.Group}}.{{.Submitter}}"{{end}}
concurrency_limits = {{.Submitter}}
{{with $x := index .Steps 0}}+IpcExe = "{{$x.Component.Name}}"{{end}}
{{with $x := index .Steps 0}}+IpcExePath = "{{$x.Component.Location}}"{{end}}
should_transfer_files = YES
transfer_input_files = iplant.sh,irods-config,iplant.cmd
transfer_output_files = logs/de-transfer-trigger.log,logs/logs-stdout-output,logs/logs-stderr-output
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
	model.Job
	DC []model.VolumesFrom
	CI []model.ContainerImage
}

// GenerateIplantScript returns a string (or error) containing the contents
// of what should go into the iplant.sh script that is executed by the HTCondor
// job out on the cluster.
func GenerateIplantScript(submission *model.Job) (string, error) {
	tmpl := `{{$uuid := .InvocationID}}#!/bin/bash

set -x

readonly IPLANT_USER={{.Submitter}}
export IPLANT_USER
readonly IPLANT_EXECUTION_ID={{.InvocationID}}
export IPLANT_EXECUTION_ID
export SCRIPT_LOCATION=${BASH_SOURCE}
EXITSTATUS=0

if [ -e /data2 ]; then ls /data2; fi

mkdir -p logs

if [ ! "$?" -eq "0" ]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi

ls -al > logs/de-transfer-trigger.log

if [ ! "$?" -eq "0" ]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi

if [ -e "iplant.sh" ]; then
	mv iplant.sh logs/
fi

if [ -e "iplant.cmd" ]; then
	mv iplant.cmd logs/
fi

{{range .DataContainers}}docker pull {{.Name}}:{{.Tag}}
{{end}}{{range .ContainerImages}}docker pull {{.Name}}:{{.Tag}}
{{end}}
{{range .DataContainers}}docker create {{if or .HostPath .ContainerPath}}-v {{end}}{{if .HostPath}}{{.HostPath}}:{{end}}{{.ContainerPath}}{{if .ReadOnly}}:ro{{end}} --name {{.NamePrefix}}-{{$uuid}} {{.Name}}:{{.Tag}}
if [ ! "$?" -eq "0" ]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi

{{end}}{{range $i, $s := .Inputs}}{{$idstr := printf "%d" $i}}docker {{.Arguments $.Submitter $.FileMetadata}} 1> {{.Stdout $idstr}} 2> {{.Stderr $idstr}}
if [ ! "$?" -eq "0" ]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi
{{end}}
{{range $i, $s := .Steps}}{{$idstr := printf "%d" $i}}docker {{.Arguments $uuid}} 1> {{.Stdout $idstr}} 2> {{.Stderr $idstr}}
if [ ! "$?" -eq "0" ]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi
{{end}}
docker {{.FinalOutputArguments}} 1> logs/logs-stdout-output 2> logs/logs-stderr-output
if [ ! "$?" -eq "0" ]; then
	EXITSTATUS=1
	exit $EXITSTATUS
fi
{{range .DataContainers}}docker rm {{.NamePrefix}}-{{$uuid}}
{{end}}
hostname
ps aux
echo -----
for i in $(ls logs); do
    echo logs/$i
    cat logs/$i
    echo -----
done
exit $EXITSTATUS
`
	t, err := template.New("iplant_script").Parse(tmpl)
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

// GenerateIRODSConfig returns the contents of the irods-config file as a string.
func GenerateIRODSConfig() (string, error) {
	tmpl := `porklock.irods-host = {{.IRODSHost}}
porklock.irods-port = {{.IRODSPort}}
porklock.irods-user = {{.IRODSUser}}
porklock.irods-pass = {{.IRODSPass}}
porklock.irods-home = {{.IRODSBase}}
porklock.irods-zone = {{.IRODSZone}}
porklock.irods-resc = {{.IRODSResc}}
`
	t, err := template.New("irods_config").Parse(tmpl)
	if err != nil {
		return "", err
	}
	var buffer bytes.Buffer
	err = t.Execute(&buffer, configurate.Config)
	if err != nil {
		return "", err
	}
	return buffer.String(), err
}

// CreateSubmissionDirectory creates a directory for a submission and returns the path to it as a string.
func CreateSubmissionDirectory(s *model.Job) (string, error) {
	dirPath := s.CondorLogDirectory()
	if path.Base(dirPath) != "logs" {
		dirPath = path.Join(dirPath, "logs")
	}
	err := os.MkdirAll(dirPath, 0755)
	if err != nil {
		return "", err
	}
	return dirPath, err
}

// CreateSubmissionFiles creates the iplant.cmd and iplant.sh files inside the
// directory designated by 'dir'. The return values are the path to the iplant.cmd
// file, the path to the iplant.sh file, and any errors, in that order.
func CreateSubmissionFiles(dir string, s *model.Job) (string, string, error) {
	cmdContents, err := GenerateCondorSubmit(s)
	if err != nil {
		return "", "", err
	}
	shContents, err := GenerateIplantScript(s)
	if err != nil {
		return "", "", err
	}
	irodsContents, err := GenerateIRODSConfig()
	if err != nil {
		return "", "", err
	}
	cmdPath := path.Join(dir, "iplant.cmd")
	shPath := path.Join(dir, "iplant.sh")
	irodsPath := path.Join(dir, "irods-config")
	err = ioutil.WriteFile(cmdPath, []byte(cmdContents), 0644)
	if err != nil {
		return "", "", nil
	}
	err = ioutil.WriteFile(shPath, []byte(shContents), 0644)
	if err != nil {
		return cmdPath, "", nil
	}
	err = ioutil.WriteFile(irodsPath, []byte(irodsContents), 0644)
	return cmdPath, shPath, err
}

// CondorSubmit temporarily changes the working directory to the path designated
// by dir and runs condor_submit inside it.
func CondorSubmit(cmdPath, shPath string, s *model.Job) (string, error) {
	csPath, err := exec.LookPath("condor_submit")
	if err != nil {
		return "", err
	}
	if !path.IsAbs(csPath) {
		csPath, err = filepath.Abs(csPath)
		if err != nil {
			return "", err
		}
	}
	cmd := exec.Command(csPath, cmdPath)
	cmd.Dir = path.Dir(cmdPath)
	cmd.Env = []string{
		fmt.Sprintf("PATH=%s", configurate.Config.Path),
		fmt.Sprintf("CONDOR_CONFIG=%s", configurate.Config.CondorConfig),
	}
	output, err := cmd.CombinedOutput()
	if err != nil {
		return "", err
	}
	logger.Printf("Output of condor_submit:\n%s\n", output)
	logger.Printf("Extracted ID: %s\n", string(model.ExtractJobID(output)))
	return string(model.ExtractJobID(output)), err
}

// CondorRm stops the job specified by UUID.
func CondorRm(uuid string) (string, error) {
	crPath, err := exec.LookPath("condor_rm")
	logger.Printf("condor_rm found at %s", crPath)
	if err != nil {
		return "", err
	}
	if !path.IsAbs(crPath) {
		crPath, err = filepath.Abs(crPath)
		if err != nil {
			return "", err
		}
	}
	cl, err := clients.NewJEXEventsClient(configurate.Config.JEXEvents)
	if err != nil {
		return "", err
	}
	logger.Printf("Created a jex-events client for %s", configurate.Config.JEXEvents)
	jr, err := cl.JobRecord(uuid)
	if err != nil {
		logger.Print(err)
		return "", err
	}
	if jr.CondorID == "" {
		return "", errors.New("CondorID was blank")
	}
	logger.Printf("CondorID %s was found for job %s\n", jr.CondorID, uuid)

	cmd := exec.Command(crPath, jr.CondorID)
	cmd.Env = []string{
		fmt.Sprintf("PATH=%s", configurate.Config.Path),
		fmt.Sprintf("CONDOR_CONFIG=%s", configurate.Config.CondorConfig),
	}
	output, err := cmd.CombinedOutput()
	logger.Printf("condor_rm output for job %s:\n%s\n", jr.CondorID, string(output))
	if err != nil {
		logger.Print(err)
		return "", err
	}
	return string(output), err
}
