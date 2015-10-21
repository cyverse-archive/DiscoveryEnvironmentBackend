package main

import (
	"fmt"
	"log"
	"messaging"
	"model"
	"os"
	"os/exec"
)

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
	cmd = append(cmd, "--label")
	cmd = append(cmd, fmt.Sprintf("%s=%s", model.DockerLabelKey, uuid))
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

func createLogsDir() {
	err := os.Mkdir("logs", 0755)
	if err != nil {
		logger.Fatal(err)
	}
}

func moveIplantCmd() {
	if _, err := os.Stat("iplant.cmd"); err != nil {
		if err = os.Rename("iplant.cmd", "logs/iplant.cmd"); err != nil {
			logger.Print(err)
		}
	}
}

func createTransferTrigger() {
	transferTrigger, err := os.Create("logs/de-transfer-trigger.log")
	if err != nil {
		logger.Print(err)
	} else {
		_, err = transferTrigger.WriteString("This is only used to force HTCondor to transfer files.")
		if err != nil {
			logger.Print(err)
		}
	}
}

func pullDataContainers(job *model.Job) messaging.StatusCode {
	var err error
	status := messaging.Success
	for _, dc := range job.DataContainers() {
		err = dckr.Pull(dc.Name, dc.Tag)
		if err != nil {
			log.Print(err)
			status = messaging.StatusDockerPullFailed
			break
		}
		if status == messaging.Success {
			cmd := exec.Command("docker", DataContainerCreateArgs(&dc, job.InvocationID)...)
			err = cmd.Run()
			if err != nil {
				log.Print(err)
				status = messaging.StatusDockerCreateFailed
				break
			}
		}
	}
	return status
}

func pullContainerImages(job *model.Job) messaging.StatusCode {
	var err error
	status := messaging.Success
	for _, ci := range job.ContainerImages() {
		err = dckr.Pull(ci.Name, ci.Tag)
		if err != nil {
			log.Print(err)
			status = messaging.StatusDockerPullFailed
			break
		}
	}
	return status
}

func removeImages(job *model.Job) {
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

func transferInputs(job *model.Job) messaging.StatusCode {
	status := messaging.Success
	for _, input := range job.Inputs() {
		cmd := exec.Command("docker", input.Arguments(job.Submitter, job.FileMetadata)...)
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
			status = messaging.StatusInputFailed
			break
		}
	}
	return status
}

func executeStep(job *model.Job, step *model.Step) messaging.StatusCode {
	status := messaging.Success
	cmd := exec.Command("docker", step.Arguments()...)
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
		status = messaging.StatusStepFailed
	}
	return status
}

func transferOutputs(job *model.Job) messaging.StatusCode {
	status := messaging.Success
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
		status = messaging.StatusOutputFailed
	}
	return status
}
