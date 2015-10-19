package main

import (
	"configurate"
	"flag"
	"fmt"
	"io/ioutil"
	"logcabin"
	"messaging"
	"model"
	"os"
	"os/signal"
	"syscall"

	"github.com/streadway/amqp"
)

var (
	logger    = logcabin.New()
	version   = flag.Bool("version", false, "Print the version information")
	jobFile   = flag.String("job", "", "The path to the job description file")
	cfgPath   = flag.String("config", "", "The path to the config file")
	dockerURI = flag.String("docker", "unix:///var/run/docker.sock", "The URI for connecting to docker.")
	gitref    string
	appver    string
	builtby   string
	job       *model.Job
	dckr      *Docker
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

func cleanup(job *model.Job) {
	err := dckr.NukeContainersByLabel(model.DockerLabelKey, job.InvocationID)
	if err != nil {
		logger.Print(err)
	}
	removeImages(job)
}

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}
	if *cfgPath == "" {
		logger.Fatal("--config must be set.")
	}
	err := configurate.Init(*cfgPath)
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

	dckr, err = NewDocker(*dockerURI)
	if err != nil {
		fail(client, job, "Failed to connect to local docker socket")
		logger.Fatal(err)
	}

	// listen for orders to stop the job.
	stopsKey := fmt.Sprintf("%s.%s", messaging.StopsKey, job.InvocationID)
	client.AddConsumer(messaging.JobsExchange, "runner", stopsKey, func(d amqp.Delivery) {
		d.Ack(false)
		fail(client, job, "Received stop request")
		cleanup(job)
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

	status := messaging.Success
	status = pullDataContainers(job)

	if status == messaging.Success {
		status = pullContainerImages(job)
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

	if status != messaging.Success {
		fail(client, job, fmt.Sprintf("Job exited with a status of %d", status))
	} else {
		success(client, job)
	}

	cleanup(job)

	os.Exit(int(status))
}
