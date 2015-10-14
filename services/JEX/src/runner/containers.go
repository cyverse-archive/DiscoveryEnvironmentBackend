package main

import (
	"strings"

	"github.com/fsouza/go-dockerclient"
)

// IsContainer returns true if the provided 'name' is a container on the system
func IsContainer(name string) (bool, error) {
	opts := docker.ListContainersOptions{All: true}
	list, err := dc.ListContainers(opts)
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
func IsRunning(name string) (bool, error) {
	opts := docker.ListContainersOptions{}
	list, err := dc.ListContainers(opts)
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

// ContainersWithPrefix returns a list of container names that start with
// 'prefix'.
func ContainersWithPrefix(prefix string) ([]string, error) {
	retval := []string{}
	opts := docker.ListContainersOptions{All: true}
	list, err := dc.ListContainers(opts)
	if err != nil {
		return retval, err
	}
	for _, c := range list {
		for _, n := range c.Names {
			if strings.HasPrefix(n, prefix) {
				retval = append(retval, n)
			}
		}
	}
	return retval, nil
}
