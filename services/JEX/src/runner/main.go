package main

import (
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

func fail(client *messaging.Client, job *model.Job, msg string) error {
	return client.PublishJobUpdate(&messaging.UpdateMessage{
		Job:     job,
		State:   messaging.FailedState,
		Message: msg,
	})
}

func success(client *messaging.Client, job *model.Job) error {
	return client.PublishJobUpdate(&messaging.UpdateMessage{
		Job:   job,
		State: messaging.SucceededState,
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
	status := messaging.Success

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
	client.SetupPublishing(messaging.JobsExchange)

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

	// listen for orders to stop the job.
	stopsKey := fmt.Sprintf("%s.%s", messaging.StopsKey, job.InvocationID)
	client.AddConsumer(messaging.JobsExchange, "runner", stopsKey, func(d amqp.Delivery) {
		d.Ack(false)
		fail(client, job, "Received stop request")
		os.Exit(-1)
	})
	go func() {
		client.Listen()
	}()

	// let everyone know the job is running
	err = client.PublishJobUpdate(&messaging.UpdateMessage{
		Job:   job,
		State: messaging.RunningState,
	})
	if err != nil {
		logger.Print(err)
	}

	createLogsDir()
	createTransferTrigger()
	moveIplantCmd()
	status = pullDataContainers(job)
	if status == messaging.Success {
		status = pullDataContainers(job)
	}
	if status == messaging.Success {
		status = transferInputs(job)
	}
	for _, step := range job.Steps {
		if status == messaging.Success {
			status = executeStep(job, &step)
		}
	}
	status = transferOutputs(job)

	CleanDataContainers()

	if status != messaging.Success {
		fail(client, job, fmt.Sprintf("Job exited with a status of %d", status))
	} else {
		success(client, job)
	}
	os.Exit(int(status))
}
