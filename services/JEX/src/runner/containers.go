package main

import (
	"fmt"
	"model"
	"os"
	"strconv"
	"strings"

	"github.com/fsouza/go-dockerclient"
)

// Docker provides operations that runner needs from the docker client.
type Docker struct {
	Client *docker.Client
}

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

	createConfig.Cmd = step.Arguments(invID)

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

	for _, dev := range step.Component.Container.Devices {
		device := docker.Device{
			PathOnHost:        dev.HostPath,
			PathInContainer:   dev.ContainerPath,
			CgroupPermissions: dev.CgroupPermissions,
		}
		createHostConfig.Devices = append(createHostConfig.Devices, device)
	}

	if step.Component.Container.WorkingDirectory() != "" {
		createConfig.WorkingDir = step.Component.Container.WorkingDirectory()
	}

	for k, v := range step.Environment {
		createConfig.Env = append(createConfig.Env, fmt.Sprintf("%s=%s", k, v))
	}

	createHostConfig.LogConfig = docker.LogConfig{Type: "none"}
	createOpts.Config = createConfig
	createOpts.HostConfig = createHostConfig
	logger.Printf("%#v\n", createHostConfig)

	container, err := d.Client.CreateContainer(createOpts)
	return container, &createOpts, err
}

// Attach attaches to the container and redirects stdout and stderr to the
// files at the provided paths. Returns a Success chan that will be sent a
// struct{} when the attach completes. A struct{} must then be sent over the
// channel for the streaming to begin.
func (d *Docker) Attach(container *docker.Container, stdout, stderr *os.File) (chan struct{}, error) {
	successChan := make(chan struct{})
	opts := docker.AttachToContainerOptions{
		Container:    container.ID,
		OutputStream: stdout,
		ErrorStream:  stderr,
		Stdout:       true,
		Stderr:       true,
		Success:      successChan,
	}
	err := d.Client.AttachToContainer(opts)
	if err != nil {
		return nil, err
	}
	return successChan, err
}

// RunStep will run a job step.
func (d *Docker) RunStep(step *model.Step, invID string) (int, error) {
	//create container
	container, opts, err := d.CreateContainerFromStep(step, invID)
	if err != nil {
		return -1, err
	}
	stdoutFile, err := os.Open(step.Stdout(invID))
	if err != nil {
		return -1, err
	}
	defer stdoutFile.Close()
	stderrFile, err := os.Open(step.Stderr(invID))
	if err != nil {
		return -1, err
	}
	defer stderrFile.Close()
	logger.Println("Attaching to container")
	//attach to container, redirecting stdout and stderr to files.
	successChan, err := d.Attach(container, stdoutFile, stderrFile)
	if err != nil {
		return -1, err
	}
	logger.Println("Done attaching to container")
	//wait for attach
	logger.Println("Waiting for attach...")
	successChan <- <-successChan
	logger.Println("Done waiting for attach")
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
	//close stdout and stderr files
	return exitCode, err
}
