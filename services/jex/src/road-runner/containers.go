package main

import (
	"configurate"
	"fmt"
	"io"
	"model"
	"os"
	"strconv"
	"strings"

	"github.com/fsouza/go-dockerclient"
)

// Docker provides operations that runner needs from the docker client.
type Docker struct {
	Client        *docker.Client
	TransferImage string
}

// WORKDIR is the path to the working directory inside all of the containers
// that are run as part of a job.
const WORKDIR = "/de-app-work"

const (
	typeLabel      = "org.iplantc.containertype"
	inputContainer = iota
	dataContainer
	stepContainer
	outputContainer
)

// NewDocker returns a *Docker that connects to the docker client listening at
// 'uri'.
func NewDocker(uri string) (*Docker, error) {
	d := &Docker{}
	cl, err := docker.NewClient(uri)
	if err != nil {
		return nil, err
	}
	d.Client = cl
	return d, err
}

// IsContainer returns true if the provided 'name' is a container on the system
func (d *Docker) IsContainer(name string) (bool, error) {
	opts := docker.ListContainersOptions{All: true}
	list, err := d.Client.ListContainers(opts)
	if err != nil {
		return false, err
	}
	for _, c := range list {
		for _, n := range c.Names {
			if strings.TrimPrefix(n, "/") == name {
				return true, nil
			}
		}
	}
	return false, nil
}

// IsRunning returns true if the contain with 'name' is running.
func (d *Docker) IsRunning(name string) (bool, error) {
	opts := docker.ListContainersOptions{}
	list, err := d.Client.ListContainers(opts)
	if err != nil {
		return false, err
	}
	for _, c := range list {
		for _, n := range c.Names {
			if strings.TrimPrefix(n, "/") == name {
				return true, nil
			}
		}
	}
	return false, nil
}

// ContainersWithLabel returns the id of all containers that have the label
// "key=value" applied to it.
func (d *Docker) ContainersWithLabel(key, value string, all bool) ([]string, error) {
	filters := map[string][]string{
		"label": []string{fmt.Sprintf("%s=%s", key, value)},
	}
	opts := docker.ListContainersOptions{
		All:     all,
		Filters: filters,
	}
	list, err := d.Client.ListContainers(opts)
	if err != nil {
		return nil, err
	}
	var retval []string
	for _, c := range list {
		retval = append(retval, c.ID)
	}
	return retval, nil
}

// NukeContainer kills the container with the provided id.
func (d *Docker) NukeContainer(id string) error {
	opts := docker.RemoveContainerOptions{
		ID:            id,
		RemoveVolumes: true,
		Force:         true,
	}
	return d.Client.RemoveContainer(opts)
}

// NukeContainersByLabel kills all running containers that have the provided
// label applied to them.
func (d *Docker) NukeContainersByLabel(key, value string) error {
	containers, err := d.ContainersWithLabel(key, value, false)
	if err != nil {
		return err
	}
	for _, container := range containers {
		err = d.NukeContainer(container)
		if err != nil {
			return err
		}
	}
	return nil
}

// NukeContainerByName kills and remove the named container.
func (d *Docker) NukeContainerByName(name string) error {
	listopts := docker.ListContainersOptions{All: true}
	list, err := d.Client.ListContainers(listopts)
	if err != nil {
		return err
	}
	for _, container := range list {
		for _, n := range container.Names {
			if strings.TrimPrefix(n, "/") == name {
				return d.NukeContainer(container.ID)
			}
		}
	}
	return nil
}

// SafelyRemoveImage will delete the image with force set to false
func (d *Docker) SafelyRemoveImage(name, tag string) error {
	opts := docker.RemoveImageOptions{
		Force: false,
	}
	imageName := fmt.Sprintf("%s:%s", name, tag)
	return d.Client.RemoveImageExtended(imageName, opts)
}

// NukeImage will delete the image with force set to true.
func (d *Docker) NukeImage(name, tag string) error {
	opts := docker.RemoveImageOptions{
		Force: true,
	}
	imageName := fmt.Sprintf("%s:%s", name, tag)
	return d.Client.RemoveImageExtended(imageName, opts)
}

// Pull will pull an image indicated by name and tag. Name is in the format
// "registry/repository". If the name doesn't contain a / then the registry
// is assumed to be "base" and the provided name will be set to repository.
// This assumes that no authentication is required.
func (d *Docker) Pull(name, tag string) error {
	auth := docker.AuthConfiguration{}
	reg := "base"
	repo := name
	if strings.Contains(name, "/") {
		parts := strings.Split(name, "/")
		if strings.Contains(parts[0], ".") {
			reg = parts[0]
			repo = parts[1]
		}
	}
	opts := docker.PullImageOptions{
		Repository:   repo,
		Registry:     reg,
		Tag:          tag,
		OutputStream: logger,
	}
	return d.Client.PullImage(opts, auth)
}

// CreateContainerFromStep creates a container from a step in the a job.
func (d *Docker) CreateContainerFromStep(step *model.Step, invID string) (*docker.Container, *docker.CreateContainerOptions, error) {
	createOpts := docker.CreateContainerOptions{}
	if step.Component.Container.Name != "" {
		createOpts.Name = step.Component.Container.Name
	}
	createConfig := &docker.Config{}
	createHostConfig := &docker.HostConfig{}

	if step.Component.Container.EntryPoint != "" {
		createConfig.Entrypoint = []string{step.Component.Container.EntryPoint}
	}

	createConfig.Cmd = step.Arguments()

	if step.Component.Container.MemoryLimit != "" {
		if parsedMem, err := strconv.ParseInt(step.Component.Container.MemoryLimit, 10, 64); err == nil {
			createConfig.Memory = parsedMem
		} else {
			logger.Print(err)
		}
	}

	if step.Component.Container.CPUShares != "" {
		if parsedCPU, err := strconv.ParseInt(step.Component.Container.CPUShares, 10, 64); err == nil {
			createConfig.CPUShares = parsedCPU
		} else {
			logger.Print(err)
		}
	}

	if step.Component.Container.NetworkMode != "" {
		if step.Component.Container.NetworkMode == "none" {
			createConfig.NetworkDisabled = true
			createHostConfig.NetworkMode = "none"
		} else {
			createHostConfig.NetworkMode = step.Component.Container.NetworkMode
		}
	}

	var fullName string
	if step.Component.Container.Image.Tag != "" {
		fullName = fmt.Sprintf(
			"%s:%s",
			step.Component.Container.Image.Name,
			step.Component.Container.Image.Tag,
		)
	} else {
		fullName = step.Component.Container.Image.Name
	}

	createConfig.Image = fullName

	for _, vf := range step.Component.Container.VolumesFrom {
		createHostConfig.VolumesFrom = append(
			createHostConfig.VolumesFrom,
			fmt.Sprintf(
				"%s-%s",
				vf.NamePrefix,
				invID,
			),
		)
	}

	for _, vol := range step.Component.Container.Volumes {
		mount := docker.Mount{
			Source:      vol.HostPath,
			Destination: vol.ContainerPath,
			RW:          !vol.ReadOnly,
		}
		createConfig.Mounts = append(createConfig.Mounts, mount)
	}
	wd, err := os.Getwd()
	if err != nil {
		return nil, nil, err
	}
	localMount := docker.Mount{
		Source:      wd,
		Destination: step.Component.Container.WorkingDirectory(),
		RW:          true,
	}
	createConfig.Mounts = append(createConfig.Mounts, localMount)

	for _, dev := range step.Component.Container.Devices {
		device := docker.Device{
			PathOnHost:        dev.HostPath,
			PathInContainer:   dev.ContainerPath,
			CgroupPermissions: dev.CgroupPermissions,
		}
		createHostConfig.Devices = append(createHostConfig.Devices, device)
	}

	createConfig.WorkingDir = step.Component.Container.WorkingDirectory()

	for k, v := range step.Environment {
		createConfig.Env = append(createConfig.Env, fmt.Sprintf("%s=%s", k, v))
	}

	createConfig.Labels = make(map[string]string)
	createConfig.Labels[model.DockerLabelKey] = invID
	createConfig.Labels[typeLabel] = strconv.Itoa(stepContainer)

	createHostConfig.LogConfig = docker.LogConfig{Type: "none"}
	createOpts.Config = createConfig
	createOpts.HostConfig = createHostConfig
	container, err := d.Client.CreateContainer(createOpts)
	return container, &createOpts, err
}

// Attach attaches to the container and redirects stdout and stderr to the
// files at the provided paths. Returns a Success chan that will be sent a
// struct{} when the attach completes. A struct{} must then be sent over the
// channel for the streaming to begin.
func (d *Docker) Attach(container *docker.Container, stdout, stderr io.Writer, sentinel chan struct{}) error {
	opts := docker.AttachToContainerOptions{
		Container:    container.ID,
		Stream:       true,
		OutputStream: stdout,
		ErrorStream:  stderr,
		Stdout:       true,
		Stderr:       true,
		Success:      sentinel,
	}
	err := d.Client.AttachToContainer(opts)
	if err != nil {
		return err
	}
	return err
}

func createFile(path string) (*os.File, error) {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return os.Create(path)
	}
	return os.Open(path)
}

func (d *Docker) runContainer(container *docker.Container, opts *docker.CreateContainerOptions, stdoutFile, stderrFile *os.File) (int, error) {
	var err error
	successChan := make(chan struct{})
	go func() {
		err = d.Attach(container, stdoutFile, stderrFile, successChan)
		if err != nil {
			logger.Print(err)
		}
	}()
	successChan <- <-successChan
	//run the container
	err = d.Client.StartContainer(container.ID, opts.HostConfig)
	if err != nil {
		return -1, err
	}
	//wait for container to exit
	exitCode, err := d.Client.WaitContainer(container.ID)
	if err != nil {
		return -1, err
	}
	return exitCode, err
}

// RunStep will run the steps in a job. If a step fails, the function will
// return with a non-zero exit code. If an error occurs, the function will
// return with a non-zero exit code and a non-nil error.
func (d *Docker) RunStep(step *model.Step, invID string, idx int) (int, error) {
	stepIdx := strconv.Itoa(idx)
	container, opts, err := d.CreateContainerFromStep(step, invID)
	if err != nil {
		return -1, err
	}
	stdoutFile, err := createFile(step.Stdout(stepIdx))
	if err != nil {
		return -1, err
	}
	defer stdoutFile.Close()
	stderrFile, err := createFile(step.Stderr(stepIdx))
	if err != nil {
		return -1, err
	}
	defer stderrFile.Close()
	return d.runContainer(container, opts, stdoutFile, stderrFile)
}

// CreateDownloadContainer creates a container that can be used to download
// input files.
func (d *Docker) CreateDownloadContainer(job *model.Job, input *model.StepInput, idx string) (*docker.Container, *docker.CreateContainerOptions, error) {
	invID := job.InvocationID
	opts := docker.CreateContainerOptions{
		Config:     &docker.Config{},
		HostConfig: &docker.HostConfig{},
	}
	image, err := configurate.C.String("porklock.image")
	if err != nil {
		return nil, nil, err
	}
	tag, err := configurate.C.String("porklock.tag")
	if err != nil {
		return nil, nil, err
	}
	err = d.Pull(image, tag)
	if err != nil {
		return nil, nil, err
	}
	opts.Config.Image = fmt.Sprintf("%s:%s", image, tag)
	opts.Name = fmt.Sprintf("input-%s-%s", idx, invID)
	opts.HostConfig.LogConfig = docker.LogConfig{Type: "none"}
	wd, err := os.Getwd()
	if err != nil {
		return nil, nil, err
	}
	opts.Config.WorkingDir = WORKDIR
	opts.Config.Mounts = append(opts.Config.Mounts, docker.Mount{
		Source:      wd,
		Destination: WORKDIR,
		RW:          true,
	})
	opts.Config.Labels = make(map[string]string)
	opts.Config.Labels[model.DockerLabelKey] = invID
	opts.Config.Labels[typeLabel] = strconv.Itoa(inputContainer)
	opts.Config.Cmd = input.Arguments(job.Submitter, job.FileMetadata)
	container, err := d.Client.CreateContainer(opts)
	return container, &opts, err
}

// DownloadInputs will run the docker containers that down input files into
// the local working directory.
func (d *Docker) DownloadInputs(job *model.Job, input *model.StepInput, idx int) (int, error) {
	inputIdx := strconv.Itoa(idx)
	container, opts, err := d.CreateDownloadContainer(job, input, inputIdx)
	if err != nil {
		return -1, err
	}
	stdoutFile, err := createFile(input.Stdout(inputIdx))
	if err != nil {
		return -1, err
	}
	defer stdoutFile.Close()
	stderrFile, err := createFile(input.Stderr(inputIdx))
	if err != nil {
		return -1, err
	}
	defer stderrFile.Close()
	return d.runContainer(container, opts, stdoutFile, stderrFile)
}

// CreateUploadContainer will initialize a container that will be used to
// upload job outputs into a directory in iRODS.
func (d *Docker) CreateUploadContainer(job *model.Job) (*docker.Container, *docker.CreateContainerOptions, error) {
	opts := docker.CreateContainerOptions{
		Config:     &docker.Config{},
		HostConfig: &docker.HostConfig{},
	}
	image, err := configurate.C.String("porklock.image")
	if err != nil {
		return nil, nil, err
	}
	tag, err := configurate.C.String("porklock.tag")
	if err != nil {
		return nil, nil, err
	}
	err = d.Pull(image, tag)
	if err != nil {
		return nil, nil, err
	}
	opts.Config.Image = fmt.Sprintf("%s:%s", image, tag)
	opts.Name = fmt.Sprintf("output-%s", job.InvocationID)
	opts.HostConfig.LogConfig = docker.LogConfig{Type: "none"}
	wd, err := os.Getwd()
	if err != nil {
		return nil, nil, err
	}
	opts.Config.WorkingDir = WORKDIR
	opts.Config.Mounts = append(opts.Config.Mounts, docker.Mount{
		Source:      wd,
		Destination: WORKDIR,
		RW:          true,
	})
	opts.Config.Labels = make(map[string]string)
	opts.Config.Labels[model.DockerLabelKey] = job.InvocationID
	opts.Config.Labels[typeLabel] = strconv.Itoa(outputContainer)
	opts.Config.Cmd = job.FinalOutputArguments()
	container, err := d.Client.CreateContainer(opts)
	return container, &opts, err
}

// UploadOutputs will upload files to iRODS from the local working directory.
func (d *Docker) UploadOutputs(job *model.Job) (int, error) {
	container, opts, err := d.CreateUploadContainer(job)
	if err != nil {
		return -1, err
	}
	stdoutFile, err := createFile("logs/logs-stdout-output")
	if err != nil {
		return -1, err
	}
	defer stdoutFile.Close()
	stderrFile, err := createFile("logs/logs-stderr-output")
	if err != nil {
		return -1, err
	}
	defer stderrFile.Close()
	return d.runContainer(container, opts, stdoutFile, stderrFile)
}

// CreateDataContainer will create a data container that is required for the job.
func (d *Docker) CreateDataContainer(vf *model.VolumesFrom, invID string) (*docker.Container, *docker.CreateContainerOptions, error) {
	opts := docker.CreateContainerOptions{
		Config:     &docker.Config{},
		HostConfig: &docker.HostConfig{},
	}
	opts.Name = fmt.Sprintf("%s-%s", vf.NamePrefix, invID)
	opts.Config.Image = fmt.Sprintf("%s:%s", vf.Name, vf.Tag)
	opts.HostConfig.LogConfig = docker.LogConfig{Type: "none"}
	opts.Config.Labels = make(map[string]string)
	opts.Config.Labels[model.DockerLabelKey] = invID
	opts.Config.Labels[typeLabel] = strconv.Itoa(dataContainer)
	if vf.HostPath != "" || vf.ContainerPath != "" {
		mount := docker.Mount{}
		if vf.HostPath != "" {
			mount.Source = vf.HostPath
		}
		mount.Destination = vf.ContainerPath
		mount.RW = !vf.ReadOnly
		opts.Config.Mounts = append(opts.Config.Mounts, mount)
	}
	opts.Config.Cmd = []string{"/bin/true"}
	container, err := d.Client.CreateContainer(opts)
	return container, &opts, err
}
