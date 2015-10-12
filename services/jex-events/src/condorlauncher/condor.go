package main

import (
	"api"
	"bytes"
	"configurate"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"logcabin"
	"messaging"
	"model"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"text/template"

	"github.com/streadway/amqp"
)

var (
	logger  = logcabin.New()
	cfgPath = flag.String("config", "", "Path to the config value. Required.")
	version = flag.Bool("version", false, "Print the version information")
	gitref  string
	appver  string
	builtby string
)

func init() {
	flag.Parse()
}

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

func GenerateJobConfig() (string, error) {
	tmpl := `amqp:
	uri: {{.String "amqp.uri"}}`
	t, err := template.New("job_config").Parse(tmpl)
	if err != nil {
		return "", err
	}
	var buffer bytes.Buffer
	err = t.Execute(&buffer, configurate.C)
	if err != nil {
		return "", err
	}
	return buffer.String(), nil
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
	irodsHost, err := configurate.C.String("irods.host")
	if err != nil {
		return "", err
	}
	irodsPort, err := configurate.C.String("irods.port")
	if err != nil {
		return "", err
	}
	irodsUser, err := configurate.C.String("irods.user")
	if err != nil {
		return "", err
	}
	irodsPass, err := configurate.C.String("irods.pass")
	if err != nil {
		return "", err
	}
	irodsBase, err := configurate.C.String("irods.base")
	if err != nil {
		return "", err
	}
	irodsResc, err := configurate.C.String("irods.resc")
	if err != nil {
		return "", err
	}
	c := &irodsconfig{
		IRODSHost: irodsHost,
		IRODSPort: irodsPort,
		IRODSUser: irodsUser,
		IRODSPass: irodsPass,
		IRODSBase: irodsBase,
		IRODSResc: irodsResc,
	}
	var buffer bytes.Buffer
	err = t.Execute(&buffer, c)
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
func CreateSubmissionFiles(dir string, s *model.Job) (string, error) {
	cmdContents, err := GenerateCondorSubmit(s)
	if err != nil {
		return "", err
	}
	irodsContents, err := GenerateIRODSConfig()
	if err != nil {
		return "", err
	}
	cmdPath := path.Join(dir, "iplant.cmd")
	irodsPath := path.Join(dir, "irods-config")
	err = ioutil.WriteFile(cmdPath, []byte(cmdContents), 0644)
	if err != nil {
		return "", nil
	}
	err = ioutil.WriteFile(irodsPath, []byte(irodsContents), 0644)
	return cmdPath, err
}

func submit(cmdPath, shPath string, s *model.Job) (string, error) {
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
	pathEnv, err := configurate.C.String("condor.path_env_var")
	if err != nil {
		pathEnv = ""
	}
	condorCfg, err := configurate.C.String("condor.condor_config")
	if err != nil {
		condorCfg = ""
	}
	cmd.Env = []string{
		fmt.Sprintf("PATH=%s", pathEnv),
		fmt.Sprintf("CONDOR_CONFIG=%s", condorCfg),
	}
	output, err := cmd.CombinedOutput()
	if err != nil {
		return "", err
	}
	logger.Printf("Output of condor_submit:\n%s\n", output)
	logger.Printf("Extracted ID: %s\n", string(model.ExtractJobID(output)))
	return string(model.ExtractJobID(output)), err
}

func launch(s *model.Job) (string, error) {
	sdir, err := CreateSubmissionDirectory(s)
	if err != nil {
		log.Printf("Error creating submission directory:\n%s\n", err)
		return "", err
	}
	cmd, err := CreateSubmissionFiles(sdir, s)
	if err != nil {
		log.Printf("Error creating submission files:\n%s", err)
		return "", err
	}
	id, err := submit(cmd)
	if err != nil {
		log.Printf("Error submitting job:\n%s", err)
		return "", err
	}
	logger.Printf("Condor job id is %s\n", id)
	return id, err
}

func stop(s *model.Job) (string, error) {
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
	pathEnv, err := configurate.C.String("condor.path_env_var")
	if err != nil {
		pathEnv = ""
	}
	condorConfig, err := configurate.C.String("condor.condor_config")
	if err != nil {
		condorConfig = ""
	}
	cmd := exec.Command(crPath, s.CondorID)
	cmd.Env = []string{
		fmt.Sprintf("PATH=%s", pathEnv),
		fmt.Sprintf("CONDOR_CONFIG=%s", condorConfig),
	}
	output, err := cmd.CombinedOutput()
	logger.Printf("condor_rm output for job %s:\n%s\n", s.CondorID, string(output))
	if err != nil {
		return "", err
	}
	return string(output), err
}

// AppVersion prints version information to stdout
func AppVersion() {
	if appver != "" {
		fmt.Printf("App-Version: %s\n", appver)
	}
	if gitref != "" {
		fmt.Printf("Git-Ref: %s\n", gitref)
	}

	if builtby != "" {
		fmt.Printf("Built-By: %s\n", builtby)
	}
}

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}
	if *cfgPath == "" {
		fmt.Println("Error: --config must be set.")
		flag.PrintDefaults()
		os.Exit(-1)
	}
	err := configurate.Init(*cfgPath)
	if err != nil {
		logger.Print(err)
		os.Exit(-1)
	}
	logger.Println("Done reading config.")

	uri, err := configurate.C.String("amqp.uri")
	if err != nil {
		log.Fatal(err)
	}
	client := messaging.NewClient(uri)
	defer client.Close()

	// Accept and handle messages sent out with the jobs.launches routing key.
	client.AddConsumer(api.JobsExchange, "condor_launches", api.LaunchesKey, func(d amqp.Delivery) {
		body := d.Body
		d.Ack(false)
		req := api.JobRequest{}
		err := json.Unmarshal(body, &req)
		if err != nil {
			logger.Print(err)
			logger.Print(string(body[:]))
			return
		}
		switch req.Command {
		case api.Launch:
			jobID, err := launch(req.Job)
			if err != nil {
				log.Print(err)
			}
			log.Printf("Launched Condor ID %s", jobID)
		case api.Stop:
			output, err := stop(req.Job)
			if err != nil {
				log.Print(err)
			}
			log.Printf("Output of the stop for %s:\n%s", req.Job.CondorID, output)
		}
	})
	client.Listen()
}
