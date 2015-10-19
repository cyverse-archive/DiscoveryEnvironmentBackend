package model

import (
	"configurate"
	"fmt"
)

// Volume describes how a local path is mounted into a container.
type Volume struct {
	HostPath      string `json:"host_path"`
	ContainerPath string `json:"container_path"`
	ReadOnly      bool   `json:"read_only"`
	Mode          string `json:"mode"`
}

// Device describes the mapping between a host device and the container device.
type Device struct {
	HostPath          string `json:"host_path"`
	ContainerPath     string `json:"container_path"`
	CgroupPermissions string `json:"cgroup_permissions"`
}

// VolumesFrom describes a container that volumes are imported from.
type VolumesFrom struct {
	Tag           string `json:"tag"`
	Name          string `json:"name"`
	NamePrefix    string `json:"name_prefix"`
	URL           string `json:"url"`
	HostPath      string `json:"host_path"`
	ContainerPath string `json:"container_path"`
	ReadOnly      bool   `json:"read_only"`
}

// ContainerImage describes a docker container image.
type ContainerImage struct {
	ID   string `json:"id"`
	Name string `json:"name"`
	Tag  string `json:"tag"`
	URL  string `json:"url"`
}

// Container describes a container used as part of a DE job.
type Container struct {
	ID          string         `json:"id"`
	Volumes     []Volume       `json:"container_volumes"`
	Devices     []Device       `json:"container_devices"`
	VolumesFrom []VolumesFrom  `json:"container_volumes_from"`
	Name        string         `json:"name"`
	NetworkMode string         `json:"network_mode"`
	CPUShares   string         `json:"cpu_shares"`
	MemoryLimit string         `json:"memory_limit"`
	Image       ContainerImage `json:"image"`
	EntryPoint  string         `json:"entrypoint"`
	WorkingDir  string         `json:"working_directory"`
}

// HasVolumes returns true if the container has volumes associated with it.
func (c *Container) HasVolumes() bool {
	return len(c.Volumes) > 0
}

// HasDevices returns true if the container has devices associated with it.
func (c *Container) HasDevices() bool {
	return len(c.Devices) > 0
}

// HasVolumesFrom returns true if the container has volumes from associated with
// it.
func (c *Container) HasVolumesFrom() bool {
	return len(c.VolumesFrom) > 0
}

// WorkingDirectory returns the container's working directory. Defaults to
// /de-app-work if the job submission didn't specify one. Use this function
// rather than accessing the field directly.
func (c *Container) WorkingDirectory() string {
	if c.WorkingDir == "" {
		return "/de-app-work"
	}
	return c.WorkingDir
}

// WorkingDirectoryOption returns a string containing a Docker command-line option
// setting the working directory.
func (c *Container) WorkingDirectoryOption() []string {
	return []string{"-w", c.WorkingDirectory()}
}

// VolumeOptions returns a string containing the docker command-line options that
// set all of the defined volumes.
func (c *Container) VolumeOptions() []string {
	retval := []string{"-v", fmt.Sprintf("$(pwd):%s", c.WorkingDirectory())}
	if c.HasVolumes() {
		for _, v := range c.Volumes {
			if v.HostPath != "" {
				retval = append(retval, "-v")
				retval = append(retval, fmt.Sprintf("%s:%s", v.HostPath, v.ContainerPath))
			} else {
				retval = append(retval, "-v")
				retval = append(retval, v.ContainerPath)
			}
		}
	}
	return retval
}

// DeviceOptions returns a string containing the docker command-line options
// that set all of the defined devices.
func (c *Container) DeviceOptions() []string {
	retval := []string{}
	if c.HasDevices() {
		for _, d := range c.Devices {
			retval = append(retval, fmt.Sprintf("--device=%s:%s", d.HostPath, d.ContainerPath))
		}
	}
	return retval
}

// VolumesFromOptions returns a string containing the docker command-line options
// that set all of the defined volumes-from.
func (c *Container) VolumesFromOptions(prefix string) []string {
	retval := []string{}
	if c.HasVolumesFrom() {
		for _, vf := range c.VolumesFrom {
			retval = append(retval, fmt.Sprintf("--volumes-from=%s-%s", prefix, vf.NamePrefix))
		}
	}
	return retval
}

// NameOption returns a string containing the docker command-line option
// that sets the container name.
func (c *Container) NameOption() []string {
	if c.Name != "" {
		return []string{"--name", fmt.Sprintf("%s", c.Name)}
	}
	return []string{}
}

// NetworkModeOption returns a string containing the docker command-line option
// that sets the container network mode.
func (c *Container) NetworkModeOption() string {
	if c.NetworkMode != "" {
		return fmt.Sprintf("--net=%s", c.NetworkMode)
	}
	return "--net=bridge"
}

// CPUSharesOption returns a string containing the docker command-line option
// that sets the number of cpu shares the container is allotted.
func (c *Container) CPUSharesOption() string {
	if c.CPUShares != "" {
		return fmt.Sprintf("--cpu-shares=%s", c.CPUShares)
	}
	return ""
}

// MemoryLimitOption returns a string containing the docker command-line option
// that sets the maximum amount of host memory that the container may use.
func (c *Container) MemoryLimitOption() string {
	if c.MemoryLimit != "" {
		return fmt.Sprintf("--memory=%s", c.MemoryLimit)
	}
	return ""
}

// IsDEImage returns true if container image is one of the DE image that requires
// special tag logic.
func (c *Container) IsDEImage() bool {
	deImages := []string{
		"discoenv/porklock",
		"discoenv/curl-wrapper",
		"gims.iplantcollaborative.org:5000/backwards-compat",
		"discoenv/backwards-compat",
	}
	actualName := c.Image.Name
	found := false
	for _, d := range deImages {
		if actualName == d {
			found = true
		}
	}
	return found
}

// Tag returns a string containing the correct tag to use with this image. The
// tag will be prefixed with a ':' unless the image is neither a DE image nor
// has the Tag field set.
func (c *Container) Tag() string {
	tag, err := configurate.C.String("condor.porklock_tag")
	if err != nil {
		tag = ""
	}
	if c.IsDEImage() {
		return fmt.Sprintf(":%s", tag)
	} else if c.Image.Tag != "" {
		return fmt.Sprintf(":%s", c.Image.Tag)
	}
	return ""
}

// ImageOption returns a string with the docker command-line option that sets
// the container image in it.
func (c *Container) ImageOption() string {
	return fmt.Sprintf("%s%s", c.Image.Name, c.Tag())
}

// EntryPointOption returns a docker command-line option that sets the
// entrypoint.
func (c *Container) EntryPointOption() string {
	if c.EntryPoint != "" {
		return fmt.Sprintf("--entrypoint=%s", c.EntryPoint)
	}
	return ""
}
