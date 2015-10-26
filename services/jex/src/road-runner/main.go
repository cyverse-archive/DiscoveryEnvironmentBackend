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
	"strconv"
	"strings"
	"syscall"
	"time"

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
	go func() {
		sig := <-c
		logger.Println("Received signal:", sig)
		if dckr == nil {
			logger.Println("Docker client is nil, can't clean up. Probably don't need to.")
		}
		if job == nil {
			logger.Println("Info didn't get parsed from the job file, can't clean up. Probably don't need to.")
		}
		if dckr != nil && job != nil {
			cleanup(job)
		}
		os.Exit(-1)
	}()
}

func init() {
	flag.Parse()
	signals()
}

// Environment returns a []string containing the environment variables that
// need to get set for every job.
func Environment(job *model.Job) []string {
	current := os.Environ()
	current = append(current, fmt.Sprintf("IPLANT_USER=%s", job.Submitter))
	current = append(current, fmt.Sprintf("IPLANT_EXECUTION_ID=%s", job.InvocationID))
	return current
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
	logger.Print(msg)
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

func running(client *messaging.Client, job *model.Job, msg string) {
	err := client.PublishJobUpdate(&messaging.UpdateMessage{
		Job:     job,
		State:   messaging.RunningState,
		Message: msg,
	})
	if err != nil {
		logger.Print(err)
	}
	logger.Print(msg)
}

func cleanup(job *model.Job) {
	err := dckr.NukeContainersByLabel(model.DockerLabelKey, job.InvocationID)
	if err != nil {
		logger.Print(err)
	}
	for _, dc := range job.DataContainers() {
		err := dckr.SafelyRemoveImage(dc.Name, dc.Tag)
		if err != nil {
			logger.Print(err)
		}
	}
	for _, ci := range job.ContainerImages() {
		err := dckr.SafelyRemoveImage(ci.Name, ci.Tag)
		if err != nil {
			logger.Print(err)
		}
	}
}

// Run executes the job, and returns the exit code on the exit channel.
func Run(client *messaging.Client, dckr *Docker, exit chan messaging.StatusCode) {
	status := messaging.Success
	host, err := os.Hostname()
	if err != nil {
		logger.Print(err)
		host = "UNKNOWN"
	}
	// let everyone know the job is running
	running(client, job, fmt.Sprintf("Job %s is running on host %s", job.InvocationID, host))

	err = os.Mkdir("logs", 0755)
	if err != nil {
		logger.Print(err)
	}

	transferTrigger, err := os.Create("logs/de-transfer-trigger.log")
	if err != nil {
		logger.Print(err)
	} else {
		_, err = transferTrigger.WriteString("This is only used to force HTCondor to transfer files.")
		if err != nil {
			logger.Print(err)
		}
	}

	if _, err := os.Stat("iplant.cmd"); err != nil {
		if err = os.Rename("iplant.cmd", "logs/iplant.cmd"); err != nil {
			logger.Print(err)
		}
	}

	// Pull the data containers
	for _, dc := range job.DataContainers() {
		running(client, job, fmt.Sprintf("Pulling container image %s:%s", dc.Name, dc.Tag))
		err = dckr.Pull(dc.Name, dc.Tag)
		if err != nil {
			logger.Print(err)
			status = messaging.StatusDockerPullFailed
			running(client, job, fmt.Sprintf("Error pulling container '%s:%s': %s", dc.Name, dc.Tag, err.Error()))
			break
		}
		running(client, job, fmt.Sprintf("Done pulling container %s:%s", dc.Name, dc.Tag))
	}

	// Create the data containers
	if status == messaging.Success {
		for _, dc := range job.DataContainers() {
			running(client, job, fmt.Sprintf("Creating data container %s-%s", dc.NamePrefix, job.InvocationID))
			_, _, err := dckr.CreateDataContainer(&dc, job.InvocationID)
			if err != nil {
				logger.Print(err)
				status = messaging.StatusDockerPullFailed
				running(client, job, fmt.Sprintf("Error creating data container %s-%s", dc.NamePrefix, job.InvocationID))
				break
			}
			running(client, job, fmt.Sprintf("Done creating data container %s-%s", dc.NamePrefix, job.InvocationID))
		}
	}

	// Pull the job step containers
	if status == messaging.Success {
		for _, ci := range job.ContainerImages() {
			running(client, job, fmt.Sprintf("Pulling tool container %s:%s", ci.Name, ci.Tag))
			err = dckr.Pull(ci.Name, ci.Tag)
			if err != nil {
				logger.Print(err)
				status = messaging.StatusDockerPullFailed
				running(client, job, fmt.Sprintf("Error pulling tool container '%s:%s': %s", ci.Name, ci.Tag, err.Error()))
				break
			}
			running(client, job, fmt.Sprintf("Done pulling tool container %s:%s", ci.Name, ci.Tag))
		}
	}

	// If pulls didn't succeed then we can't guarantee that we've got the
	// correct versions of the tools. Don't bother pulling in data in that case,
	// things are already screwed up.
	if status == messaging.Success {
		for idx, input := range job.Inputs() {
			running(client, job, fmt.Sprintf("Downloading %s", input.IRODSPath()))
			exitCode, err := dckr.DownloadInputs(job, &input, idx)
			if exitCode != 0 || err != nil {
				if err != nil {
					logger.Print(err)
					running(client, job, fmt.Sprintf("Error downloading %s: %s", input.IRODSPath(), err.Error()))
				} else {
					running(client, job, fmt.Sprintf("Error downloading %s: Transfer utility exited with %d", input.IRODSPath(), exitCode))
				}
				status = messaging.StatusInputFailed
				break
			}
			running(client, job, fmt.Sprintf("Finished downloading %s", input.IRODSPath()))
		}
	}

	// Only attempt to run the steps if the input downloads succeeded. No reason
	// to run the steps if there's no/corrupted data to operate on.
	if status == messaging.Success {
		for idx, step := range job.Steps {
			running(client, job,
				fmt.Sprintf(
					"Running tool container %s:%s with arguments: %s",
					step.Component.Container.Image.Name,
					step.Component.Container.Image.Tag,
					strings.Join(step.Arguments(), " "),
				),
			)
			exitCode, err := dckr.RunStep(&step, job.InvocationID, idx)
			if exitCode != 0 || err != nil {
				if err != nil {
					logger.Print(err)
					running(client, job,
						fmt.Sprintf(
							"Error running tool container %s:%s with arguments '%s': %s",
							step.Component.Container.Image.Name,
							step.Component.Container.Image.Tag,
							strings.Join(step.Arguments(), " "),
							err.Error(),
						),
					)
				} else {
					running(client, job,
						fmt.Sprintf(
							"Tool container %s:%s with arguments '%s' exit with code: %d",
							step.Component.Container.Image.Name,
							step.Component.Container.Image.Tag,
							strings.Join(step.Arguments(), " "),
							exitCode,
						),
					)
				}
				status = messaging.StatusStepFailed
				break
			}
			running(client, job,
				fmt.Sprintf("Tool container %s:%s with arguments '%s' finished successfully",
					step.Component.Container.Image.Name,
					step.Component.Container.Image.Tag,
					strings.Join(step.Arguments(), " "),
				),
			)
		}
	}

	// Always attempt to transfer outputs. There might be logs that can help
	// debug issues when the job fails.
	running(client, job, fmt.Sprintf("Beginning to upload outputs to %s", job.OutputDirectory()))
	exitCode, err := dckr.UploadOutputs(job)
	if exitCode != 0 || err != nil {
		if err != nil {
			logger.Print(err)
			running(client, job, fmt.Sprintf("Error uploading outputs to %s: %s", job.OutputDirectory(), err.Error()))
		} else {
			running(client, job, fmt.Sprintf("Transfer utility exited with a code of %d when uploading outputs to %s", exitCode, err.Error()))
		}
		status = messaging.StatusOutputFailed
	}
	running(client, job, fmt.Sprintf("Done uploading outputs to %s", job.OutputDirectory()))

	// Always inform upstream of the job status.
	if status != messaging.Success {
		fail(client, job, fmt.Sprintf("Job exited with a status of %d", status))
	} else {
		success(client, job)
	}
	exit <- status
}

// Wait implements the logic for killing the job when the time limit is surpassed.
func Wait(client *messaging.Client, dckr *Docker, seconds chan int64, exit chan messaging.StatusCode) {
	limit := <-seconds
	durationStr := fmt.Sprintf("%ds", limit)
	duration, err := time.ParseDuration(durationStr)
	if err != nil {
		exit <- messaging.StatusBadDuration
	} else {
		time.Sleep(duration)
		logger.Printf("Time limit reached after %s", durationStr)
		exit <- messaging.StatusTimeLimit
	}
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

	// The channel that the exit code will be passed along on.
	exit := make(chan messaging.StatusCode)

	// Could probably reuse the exit channel, but that's less explicit.
	finalExit := make(chan messaging.StatusCode)

	// The channel that additional seconds will be sent through for the time limit.
	seconds := make(chan int64)

	// Clean up code based on the exit status.
	go func() {
		exitCode := <-exit
		switch exitCode {
		case messaging.StatusTimeLimit, messaging.StatusKilled:
			//Annihilate the input/steps/data containers even if they're running,
			//but allow the output containers to run. Yanking the rug out from the
			//containers should force the Run() function to 'fall through' to any clean
			//up steps.
			logger.Printf("Received an exit code of %d, cleaning up", int(exitCode))
			for _, dc := range job.DataContainers() {
				logger.Printf("Nuking image %s:%s", dc.Name, dc.Tag)
				err := dckr.NukeImage(dc.Name, dc.Tag)
				if err != nil {
					logger.Print(err)
				}
			}
			for _, ci := range job.ContainerImages() {
				logger.Printf("Nuking image %s:%s", ci.Name, ci.Tag)
				err := dckr.NukeImage(ci.Name, ci.Tag)
				if err != nil {
					logger.Print(err)
				}
			}
			logger.Println("Finding all input containers")
			inputContainers, err := dckr.ContainersWithLabel(typeLabel, strconv.Itoa(inputContainer), true)
			if err != nil {
				logger.Print(err)
				inputContainers = []string{}
			}
			for _, ic := range inputContainers {
				logger.Printf("Nuking input container %s", ic)
				err = dckr.NukeContainer(ic)
				if err != nil {
					logger.Print(err)
				}
			}
			logger.Println("Finding all step containers")
			stepContainers, err := dckr.ContainersWithLabel(typeLabel, strconv.Itoa(stepContainer), true)
			if err != nil {
				logger.Print(err)
				inputContainers = []string{}
			}
			for _, sc := range stepContainers {
				logger.Printf("Nuking step container %s", sc)
				err = dckr.NukeContainer(sc)
				if err != nil {
					logger.Print(err)
				}
			}
			logger.Println("Finding all data containers")
			dataContainers, err := dckr.ContainersWithLabel(typeLabel, strconv.Itoa(dataContainer), true)
			if err != nil {
				logger.Print(err)
				inputContainers = []string{}
			}
			for _, dc := range dataContainers {
				logger.Printf("Nuking data container %s", dc)
				err = dckr.NukeContainer(dc)
				if err != nil {
					logger.Print(err)
				}
			}

			//wait for the exit code from the Run function.
			<-exit

			//Aggressively clean up the rest of the job.
			logger.Printf("Nuking all containers with the label %s=%s", model.DockerLabelKey, job.InvocationID)
			err = dckr.NukeContainersByLabel(model.DockerLabelKey, job.InvocationID)
			if err != nil {
				logger.Print(err)
			}

		default:
			logger.Printf("Received an exit code of %d, cleaning up", int(exitCode))
			logger.Printf("Finding all containers with the label %s=%s", model.DockerLabelKey, job.InvocationID)
			jobContainers, err := dckr.ContainersWithLabel(model.DockerLabelKey, job.InvocationID, true)
			if err != nil {
				logger.Print(err)
				jobContainers = []string{}
			}
			for _, jc := range jobContainers {
				logger.Printf("Nuking container %s", jc)
				err = dckr.NukeContainer(jc)
				if err != nil {
					logger.Print(err)
				}
			}
			for _, dc := range job.DataContainers() {
				logger.Printf("Safely removing image %s:%s", dc.Name, dc.Tag)
				err := dckr.SafelyRemoveImage(dc.Name, dc.Tag)
				if err != nil {
					logger.Print(err)
				}
			}
			for _, ci := range job.ContainerImages() {
				logger.Printf("Safely removing image %s:%s", ci.Name, ci.Tag)
				err := dckr.SafelyRemoveImage(ci.Name, ci.Tag)
				if err != nil {
					logger.Print(err)
				}
			}
		}
		finalExit <- exitCode
	}()

	// listen for orders to stop the job.
	stopsKey := fmt.Sprintf("%s.%s", messaging.StopsKey, job.InvocationID)
	client.AddConsumer(messaging.JobsExchange, "runner", stopsKey, func(d amqp.Delivery) {
		d.Ack(false)
		running(client, job, "Received stop request")
		exit <- -1
	})

	go func() {
		client.Listen()
	}()

	go Wait(client, dckr, seconds, exit)
	seconds <- job.TimeLimit
	go Run(client, dckr, exit)
	os.Exit(int(<-finalExit))
}
