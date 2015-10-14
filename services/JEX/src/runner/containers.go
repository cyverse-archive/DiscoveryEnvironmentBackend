package main

import (
	"fmt"
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
			if n == name {
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
			if n == name {
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
