package main

import (
	"api"
	"configurate"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"logcabin"
	"messaging"
	"model"
	"os"
	"os/exec"
	"os/signal"
	"syscall"

	"github.com/fsouza/go-dockerclient"
	"github.com/streadway/amqp"
)

var (
	logger  = logcabin.New()
	version = flag.Bool("version", false, "Print the version information")
	jobFile = flag.String("job", "", "The path to the job description file")
	cfgPath = flag.String("config", "", "The path to the config file")
	gitref  string
	appver  string
	builtby string
	job     *model.Job
	dc      *docker.Client
)

func signals() {
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, os.Kill, syscall.SIGTERM, syscall.SIGSTOP, syscall.SIGQUIT)
}

func init() {
	flag.Parse()
	signals()
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

// Environment returns a []string containing the environment variables that
// need to get set for every job.
func Environment(job *model.Job) []string {
	current := os.Environ()
	current = append(current, fmt.Sprintf("IPLANT_USER=%s", job.Submitter))
	current = append(current, fmt.Sprintf("IPLANT_EXECUTION_ID=%s", job.InvocationID))
	return current
}

//DataContainerPullArgs returns a string containing the command to pull a data container.
func DataContainerPullArgs(dc *model.VolumesFrom) []string {
	cmd := []string{
		"pull",
		fmt.Sprintf("%s:%s", dc.Name, dc.Tag),
	}
	return cmd
}

// ContainerImagePullArgs returns a []string containing the args to an
// exec.Command for pulling container images.
func ContainerImagePullArgs(ci *model.ContainerImage) []string {
	cmd := []string{
		"pull",
		fmt.Sprintf("%s:%s", ci.Name, ci.Tag),
	}
	return cmd
}

// DataContainerCreateArgs returns a []string containing the args to an
// exec.Command for creating data containers.
func DataContainerCreateArgs(dc *model.VolumesFrom, uuid string) []string {
	cmd := []string{
		"create",
	}
	if dc.HostPath != "" || dc.ContainerPath != "" {
		cmd = append(cmd, "-v")
		var v string
		if dc.HostPath != "" {
			v = fmt.Sprintf("%s:%s", dc.HostPath, dc.ContainerPath)
		} else {
			v = dc.ContainerPath
		}
		if dc.ReadOnly {
			v = fmt.Sprintf("%s:ro", v)
		}
		cmd = append(cmd, v)
	}
	cmd = append(cmd, "--name")
	cmd = append(cmd, fmt.Sprintf("%s-%s", dc.NamePrefix, uuid))
	cmd = append(cmd, fmt.Sprintf("%s:%s", dc.Name, dc.Tag))
	return cmd
}

// CleanDataContainers cleans out the data containers created for this job.
func CleanDataContainers() {
	for _, dc := range job.DataContainers() {
		args := []string{"rm", fmt.Sprintf("%s-%s", dc.NamePrefix, job.InvocationID)}
		cmd := exec.Command("docker", args...)
		err := cmd.Run()
		if err != nil {
			log.Print(err)
		}
	}
}

// CleanJobContainers will kill and remove any containers that are associated with
// the job.
func CleanJobContainers() {

}

func fail(client *messaging.Client, job *model.Job, msg string) error {
	return client.PublishJobUpdate(&api.UpdateMessage{
		Job:     job,
		State:   api.FailedState,
		Message: msg,
	})
}

func success(client *messaging.Client, job *model.Job) error {
	return client.PublishJobUpdate(&api.UpdateMessage{
		Job:   job,
		State: api.SucceededState,
	})
}

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}
	if *cfgPath == "" {
		logger.Fatal("--config must be set.")
	}
	var err error
	status := api.Success

	err = configurate.Init(*cfgPath)
	if err != nil {
		logger.Fatal(err)
	}
	uri, err := configurate.C.String("amqp.uri")
	if err != nil {
		logger.Fatal(err)
	}
	client := messaging.NewClient(uri)
	defer client.Close()
	client.SetupPublishing(api.JobsExchange)

	if *jobFile == "" {
		logger.Fatal("--job must be set.")
	}
	data, err := ioutil.ReadFile(*jobFile)
	if err != nil {
		logger.Fatal(err)
	}
	job, err = model.NewFromData(data)
	if err != nil {
		logger.Fatal(err)
	}

	dc, err = docker.NewClient("unix:///var/run/docker.sock")
	if err != nil {
		fail(client, job, "Failed to connect to local docker socket")
		logger.Fatal(err)
	}

	stopsKey := fmt.Sprintf("%s.%s", api.StopsKey, job.InvocationID)
	client.AddConsumer(api.JobsExchange, "runner", stopsKey, func(d amqp.Delivery) {
		d.Ack(false)
		fail(client, job, "Received stop request")
		os.Exit(-1)
	})
	go func() {
		client.Listen()
	}()

	err = client.PublishJobUpdate(&api.UpdateMessage{
		Job:   job,
		State: api.RunningState,
	})
	if err != nil {
		logger.Print(err)
	}

	// create the logs directory
	err = os.Mkdir("logs", 0755)
	if err != nil {
		logger.Fatal(err)
	}

	// create the de-transfer-trigger
	transferTrigger, err := os.Create("logs/de-transfer-trigger.log")
	if err != nil {
		log.Print(err)
	} else {
		_, err = transferTrigger.WriteString("This is only used to force HTCondor to transfer files.")
		if err != nil {
			log.Print(err)
		}
	}

	// mv iplant.cmd into the logs directory
	if _, err = os.Stat("iplant.cmd"); err != nil {
		if err = os.Rename("iplant.cmd", "logs/iplant.cmd"); err != nil {
			log.Print(err)
		}
	}

	// pull the data containers
	for _, dc := range job.DataContainers() {
		cmd := exec.Command("docker", DataContainerPullArgs(&dc)...)
		err = cmd.Run()
		if err != nil {
			log.Print(err)
			status = api.StatusDockerPullFailed
			break
		}
		if status == api.Success {
			cmd = exec.Command("docker", DataContainerCreateArgs(&dc, job.InvocationID)...)
			err = cmd.Run()
			if err != nil {
				log.Print(err)
				status = api.StatusDockerCreateFailed
				break
			}
		}
	}

	// pull the container images
	if status == api.Success {
		for _, ci := range job.ContainerImages() {
			cmd := exec.Command("docker", ContainerImagePullArgs(&ci)...)
			err = cmd.Run()
			if err != nil {
				log.Print(err)
				status = api.StatusDockerPullFailed
				break
			}
		}
	}

	// transfer the inputs
	if status == api.Success {
		for _, input := range job.Inputs() {
			cmd := exec.Command("docker", input.Arguments(job.Submitter, job.InvocationID, job.FileMetadata)...)
			stdout, err := os.Open(input.Stdout(job.InvocationID))
			if err != nil {
				log.Print(err)
			} else {
				cmd.Stdout = stdout
			}
			stderr, err := os.Open(input.Stderr(job.InvocationID))
			if err != nil {
				log.Print(err)
			} else {
				cmd.Stderr = stderr
			}
			err = cmd.Run()
			if err != nil {
				log.Print(err)
				status = api.StatusInputFailed
				break
			}
		}
	}

	// execute the steps
	for _, step := range job.Steps {
		if status == api.Success {
			cmd := exec.Command("docker", step.Arguments(job.InvocationID)...)
			stdout, err := os.Open(step.Stdout(job.InvocationID))
			if err != nil {
				log.Print(err)
			} else {
				cmd.Stdout = stdout
			}
			stderr, err := os.Open(step.Stderr(job.InvocationID))
			if err != nil {
				log.Print(err)
			} else {
				cmd.Stderr = stderr
			}
			cmd.Env = Environment(job)
			err = cmd.Run()
			if err != nil {
				log.Print(err)
				status = api.StatusStepFailed
				break
			}
		}
	}

	// transfer outputs should always attempt to execute, event if the job itself
	// has already failed.
	cmd := exec.Command("docker", job.FinalOutputArguments()...)
	stdout, err := os.Open("logs/logs-stdout-output")
	if err != nil {
		log.Print(err)
	} else {
		cmd.Stdout = stdout
	}
	stderr, err := os.Open("logs/logs-stderr-output")
	if err != nil {
		log.Print(err)
	} else {
		cmd.Stderr = stderr
	}
	err = cmd.Run()
	if err != nil {
		log.Print(err)
		status = api.StatusOutputFailed
	}

	CleanDataContainers()

	if status != api.Success {
		fail(client, job, fmt.Sprintf("Job exited with a status of %d", status))
	} else {
		success(client, job)
	}
	os.Exit(int(status))
}
