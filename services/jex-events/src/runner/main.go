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
)

func init() {
	flag.Parse()
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

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}
	if *cfgPath == "" {
		logger.Fatal("--config must be set.")
	}
	if *jobFile == "" {
		logger.Fatal("--job must be set.")
	}
	err := configurate.Init(*cfgPath)
	if err != nil {
		logger.Fatal(err)
	}
	uri, err := configurate.C.String("amqp.uri")
	if err != nil {
		logger.Fatal(err)
	}
	data, err := ioutil.ReadFile(*jobFile)
	if err != nil {
		logger.Fatal(err)
	}
	job, err := model.NewFromData(data)
	if err != nil {
		logger.Fatal(err)
	}
	client := messaging.NewClient(uri)
	defer client.Close()
	client.SetupPublishing(api.JobsExchange)
	stopsKey := fmt.Sprintf("%s.%s", api.StopsKey, job.InvocationID)
	client.AddConsumer(api.JobsExchange, "runner", stopsKey, func(d amqp.Delivery) {
		d.Ack(false)
		logger.Println("i ded")
		os.Exit(-1)
	})
	go func() {
		client.Listen()
	}()

	// export environment variables
	// create the logs directory
	err = os.Mkdir("logs", 0755)
	if err != nil {
		log.Fatal(err)
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
		}
		cmd = exec.Command("docker", DataContainerCreateArgs(&dc, job.InvocationID)...)
		err = cmd.Run()
		if err != nil {
			log.Print(err)
		}
	}

	// pull the container images
	for _, ci := range job.ContainerImages() {
		cmd := exec.Command("docker", ContainerImagePullArgs(&ci)...)
		err = cmd.Run()
		if err != nil {
			log.Print(err)
		}
	}

	// create the data containers
	// transfer the inputs
	// execute the steps
	// transfer outputs
	// remove the data containers
}
