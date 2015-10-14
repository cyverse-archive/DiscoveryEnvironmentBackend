package model

import (
	"reflect"
	"testing"
)

func TestStepContainerID(t *testing.T) {
	s := inittests(t)
	container := s.Steps[0].Component.Container
	if container.ID != "16fd2a16-3ac6-11e5-a25d-2fa4b0893ef1" {
		t.Errorf("The step's container ID was '%s' when it should have been '16fd2a16-3ac6-11e5-a25d-2fa4b0893ef1'", container.ID)
	}
}

func TestStepContainerImageID(t *testing.T) {
	s := inittests(t)
	image := s.Steps[0].Component.Container.Image
	if image.ID != "fc210a84-f7cd-4067-939c-a68ec3e3bd2b" {
		t.Errorf("The container image ID was '%s' when it should have been 'fc210a84-f7cd-4067-939c-a68ec3e3bd2b'", image.ID)
	}
}

func TestStepContainerImageURL(t *testing.T) {
	s := inittests(t)
	image := s.Steps[0].Component.Container.Image
	if image.URL != "https://registry.hub.docker.com/u/discoenv/backwards-compat" {
		t.Errorf("The container image URL was '%s' when it should have been 'https://registry.hub.docker.com/u/discoenv/backwards-compat'", image.URL)
	}
}

func TestStepContainerImageTag(t *testing.T) {
	s := inittests(t)
	image := s.Steps[0].Component.Container.Image
	if image.Tag != "latest" {
		t.Errorf("The container image tag was '%s' when it should have been 'latest'", image.Tag)
	}
}

func TestStepContainerImageName(t *testing.T) {
	s := inittests(t)
	image := s.Steps[0].Component.Container.Image
	if image.Name != "gims.iplantcollaborative.org:5000/backwards-compat" {
		t.Errorf("The container image name was '%s' when it should have been 'gims.iplantcollaborative.org:5000/backwards-compat'", image.Name)
	}
}

func TestStepContainerVolumesFrom1(t *testing.T) {
	s := inittests(t)
	vfs := s.Steps[0].Component.Container.VolumesFrom
	vfsLength := len(vfs)
	if vfsLength != 2 {
		t.Errorf("The number of VolumesFrom entries was '%d' when it should have been '2'", vfsLength)
	}
}

func TestStepContainerVolumesFrom2(t *testing.T) {
	s := inittests(t)
	vfs := s.Steps[0].Component.Container.VolumesFrom[0]
	if vfs.Name != "vf-name1" {
		t.Errorf("The VolumesFrom name was '%s' when it should have been 'vf-name1'", vfs.Name)
	}
	if vfs.NamePrefix != "vf-prefix1" {
		t.Errorf("The VolumesFrom prefix was '%s' when it should have been 'vf-prefix1'", vfs.NamePrefix)
	}
	if vfs.Tag != "vf-tag1" {
		t.Errorf("The VolumesFrom tag was '%s' when it should have been 'vf-tag1'", vfs.Tag)
	}
	if vfs.URL != "vf-url1" {
		t.Errorf("The VolumesFrom url was '%s' when it should have been 'vf-url1'", vfs.URL)
	}
	if vfs.HostPath != "/host/path1" {
		t.Errorf("The VolumesFrom host path was '%s' when it should have been '/host/path1'", vfs.HostPath)
	}
	if vfs.ContainerPath != "/container/path1" {
		t.Errorf("The VolumesFrom container path was '%s' when it should have been '/container/path1'", vfs.ContainerPath)
	}
	if !vfs.ReadOnly {
		t.Error("The VolumesFrom read-only field was false when it should have been true.")
	}
}

func TestStepContainerVolumesFrom3(t *testing.T) {
	s := inittests(t)
	vfs := s.Steps[0].Component.Container.VolumesFrom[1]
	if vfs.Name != "vf-name2" {
		t.Errorf("The VolumesFrom name was '%s' when it should have been 'vf-name2'", vfs.Name)
	}
	if vfs.NamePrefix != "vf-prefix2" {
		t.Errorf("The VolumesFrom prefix was '%s' when it should have been 'vf-prefix2'", vfs.NamePrefix)
	}
	if vfs.Tag != "vf-tag2" {
		t.Errorf("The VolumesFrom tag was '%s' when it should have been 'vf-tag2'", vfs.Tag)
	}
	if vfs.URL != "vf-url2" {
		t.Errorf("The VolumesFrom url was '%s' when it should have been 'vf-url2'", vfs.URL)
	}
	if vfs.HostPath != "/host/path2" {
		t.Errorf("The VolumesFrom host path was '%s' when it should have been '/host/path2'", vfs.HostPath)
	}
	if vfs.ContainerPath != "/container/path2" {
		t.Errorf("The VolumesFrom container path was '%s' when it should have been '/container/path2'", vfs.ContainerPath)
	}
	if !vfs.ReadOnly {
		t.Error("The VolumesFrom read-only field was false when it should have been true.")
	}
}

func TestStepContainerVolumes(t *testing.T) {
	s := inittests(t)
	vols := s.Steps[0].Component.Container.Volumes
	volslen := len(vols)
	if volslen != 2 {
		t.Errorf("The number of volumes was %d when it should have been 2", volslen)
	}
	vol := vols[0]
	if vol.HostPath != "/host/path1" {
		t.Errorf("The volume host path was set to '%s' instead of '/host/path1'", vol.HostPath)
	}
	if vol.ContainerPath != "/container/path1" {
		t.Errorf("The volume container path was set to '%s' instead of '/container/path1'", vol.ContainerPath)
	}
	vol = vols[1]
	if vol.HostPath != "" {
		t.Errorf("The volume host path was set to '%s' instead of an empty string", vol.HostPath)
	}
	if vol.ContainerPath != "/container/path2" {
		t.Errorf("The volume container path was set to '%s' instead of '/container/path2'", vol.ContainerPath)
	}
}

func TestStepContainerDevices(t *testing.T) {
	s := inittests(t)
	devices := s.Steps[0].Component.Container.Devices
	numdevices := len(devices)
	if numdevices != 2 {
		t.Errorf("The number of devices was %d when it should have been 2", devices)
	}
	device := devices[0]
	if device.HostPath != "/host/path1" {
		t.Errorf("The volume host path was set to '%s' instead of '/host/path1'", device.HostPath)
	}
	if device.ContainerPath != "/container/path1" {
		t.Errorf("The volume container path was set to '%s' instead of '/container/path1'", device.ContainerPath)
	}
	device = devices[1]
	if device.HostPath != "/host/path2" {
		t.Errorf("The volume host path was set to '%s' instead of '/host/path2'", device.HostPath)
	}
	if device.ContainerPath != "/container/path2" {
		t.Errorf("The volume container path was set to '%s' instead of '/container/path2'", device.ContainerPath)
	}
}

func TestStepContainerWorkingDir(t *testing.T) {
	s := inittests(t)
	w := s.Steps[0].Component.Container.WorkingDir
	if w != "/work" {
		t.Errorf("The working directory for the container was '%s' instead of '/work'", w)
	}
}

func TestStepContainerWorkingDirectory(t *testing.T) {
	s := _inittests(t, false)
	w := s.Steps[0].Component.Container.WorkingDirectory()
	if w != "/work" {
		t.Errorf("The return value of WorkingDirectory() was '%s' instead of '/work'", w)
	}
	s.Steps[0].Component.Container.WorkingDir = ""
	w = s.Steps[0].Component.Container.WorkingDirectory()
	if w != "/de-app-work" {
		t.Errorf("The return value of WorkingDirectory was '%s' instead of '/de-app-work'", w)
	}
}

func TestStepContainerWorkingDirectoryOption(t *testing.T) {
	s := _inittests(t, false)
	actual := s.Steps[0].Component.Container.WorkingDirectoryOption()
	expected := []string{"-w", "/work"}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("WorkingDirectoryOption() returned %#v instead of %#v", actual, expected)
	}
	s.Steps[0].Component.Container.WorkingDir = ""
	actual = s.Steps[0].Component.Container.WorkingDirectoryOption()
	expected = []string{"-w", "/de-app-work"}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("WorkingDirectoryOption() returned %#v instead of %#v", actual, expected)
	}
}

func TestVolumeOptions(t *testing.T) {
	s := _inittests(t, false)
	actual := s.Steps[0].Component.Container.VolumeOptions()
	expected := []string{"-v", "$(pwd):/work", "-v", "/host/path1:/container/path1", "-v", "/container/path2"}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf(
			"The volume option was: \n\t%#v\nrather than:\n\t%#v",
			actual,
			expected,
		)
	}
}

func TestVolumesFromOptions(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Component.Container.VolumesFromOptions("test")
	expected := []string{"--volumes-from=test-vf-prefix1", "--volumes-from=test-vf-prefix2"}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("The volumes-from options were:\n\t%#v\ninstead of:\n\t%#v", actual, expected)
	}
}

func TestNameOption(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Component.Container.NameOption()
	expected := []string{"--name", "test-name"}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("The container name was '%s' when it should have been '%s'", actual, expected)
	}
}

func TestNetworkModeOption(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Component.Container.NetworkModeOption()
	expected := "--net=none"
	if actual != expected {
		t.Errorf("The container network mode was '%s' instead of '%s'", actual, expected)
	}
}

func TestCPUSharesOption(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Component.Container.CPUSharesOption()
	expected := "--cpu-shares=2048"
	if actual != expected {
		t.Errorf("The container cpu shares was '%s' instead of '%s'", actual, expected)
	}
}

func TestMemoryLimitOption(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Component.Container.MemoryLimitOption()
	expected := "--memory=2048M"
	if actual != expected {
		t.Errorf("The container memory limit was '%s' instead of '%s'", actual, expected)
	}
}

func TestTag(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Component.Container.Tag()
	expected := ":test"
	if actual != expected {
		t.Errorf("Tag() returned '%s' instead of '%s'", actual, expected)
	}
	s.Steps[0].Component.Container.Image.Name = "discoenv/test"
	s.Steps[0].Component.Container.Image.Tag = "dev"
	actual = s.Steps[0].Component.Container.Tag()
	expected = ":dev"
	if actual != expected {
		t.Errorf("Tag() returned '%s' instead of '%s'", actual, expected)
	}
	s.Steps[0].Component.Container.Image.Tag = ""
	actual = s.Steps[0].Component.Container.Tag()
	expected = ""
	if actual != expected {
		t.Errorf("Tag() returned '%s' instead of '%s'", actual, expected)
	}
	s = _inittests(t, false)
}

func TestImageOption(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Component.Container.ImageOption()
	expected := "gims.iplantcollaborative.org:5000/backwards-compat:test"
	if actual != expected {
		t.Errorf("ImageOption() returned '%s' instead of '%s'", actual, expected)
	}
	s.Steps[0].Component.Container.Image.Name = "discoenv/test"
	s.Steps[0].Component.Container.Image.Tag = "dev"
	actual = s.Steps[0].Component.Container.ImageOption()
	expected = "discoenv/test:dev"
	if actual != expected {
		t.Errorf("ImageOption() returned '%s' instead of '%s'", actual, expected)
	}
	s.Steps[0].Component.Container.Image.Tag = ""
	actual = s.Steps[0].Component.Container.ImageOption()
	expected = "discoenv/test"
	if actual != expected {
		t.Errorf("ImageOption() returned '%s' instead of '%s'", actual, expected)
	}
	_inittests(t, false)
}

func TestEntryPointOption(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Component.Container.EntryPointOption()
	expected := "--entrypoint=/bin/true"
	if actual != expected {
		t.Errorf("ImageOption() returned '%s' instead of '%s'", actual, expected)
	}
	s.Steps[0].Component.Container.EntryPoint = ""
	actual = s.Steps[0].Component.Container.EntryPointOption()
	expected = ""
	if actual != expected {
		t.Errorf("ImageOption() returned '%s' instead of '%s'", actual, expected)
	}
	_inittests(t, false)
}

func TestIsDEImage(t *testing.T) {
	s := inittests(t)
	actual := s.Steps[0].Component.Container.IsDEImage()
	if !actual {
		t.Error("IsDEImage() returned false instead of true")
	}
}

func TestDevices(t *testing.T) {
	s := inittests(t)
	numdevices := len(s.Steps[0].Component.Container.Devices)
	if numdevices != 2 {
		t.Errorf("The number of devices was '%d' rather than '2'", numdevices)
	}
	d1 := s.Steps[0].Component.Container.Devices[0]
	if d1.HostPath != "/host/path1" {
		t.Errorf("The first device's host path was '%s' instead of '/host/path1'", d1.HostPath)
	}
	if d1.ContainerPath != "/container/path1" {
		t.Errorf("The first device's container path was '%s' instead of '/container/path1'", d1.ContainerPath)
	}
	d2 := s.Steps[0].Component.Container.Devices[1]
	if d2.HostPath != "/host/path2" {
		t.Errorf("The second device's host path was '%s' instead of '/host/path2'", d2.HostPath)
	}
	if d2.ContainerPath != "/container/path2" {
		t.Errorf("The second device's container path was '%s' instead of '/container/path1'", d2.ContainerPath)
	}
	actual := s.Steps[0].Component.Container.DeviceOptions()
	expected := []string{"--device=/host/path1:/container/path1", "--device=/host/path2:/container/path2"}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("The device option was:\n\t%s\nrather than:\n\t%s", actual, expected)
	}
}
