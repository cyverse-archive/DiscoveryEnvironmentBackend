package submissions

import (
	"bytes"
	"configurate"
	"encoding/json"
	"fmt"
	"log"
	"path"
	"regexp"
	"strings"
	"time"
)

// Volume describes how a local path is mounted into a container.
type Volume struct {
	HostPath      string `json:"host_path"`
	ContainerPath string `json:"container_path"`
}

// Device describes the mapping between a host device and the container device.
type Device struct {
	HostPath      string `json:"host_path"`
	ContainerPath string `json:"container_path"`
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
func (c *Container) WorkingDirectoryOption() string {
	return fmt.Sprintf("-w %s", c.WorkingDirectory())
}

// VolumeOptions returns a string containing the docker command-line options that
// set all of the defined volumes.
func (c *Container) VolumeOptions() string {
	var buffer bytes.Buffer
	buffer.WriteString(fmt.Sprintf("-v $(pwd):%s", c.WorkingDirectory()))
	if c.HasVolumes() {
		for _, v := range c.Volumes {
			buffer.WriteString(" ")
			if v.HostPath != "" {
				buffer.WriteString(fmt.Sprintf("-v %s:%s", v.HostPath, v.ContainerPath))
			} else {
				buffer.WriteString(fmt.Sprintf("-v %s", v.ContainerPath))
			}
		}
	}
	return buffer.String()
}

// DeviceOptions returns a string containing the docker command-line options
// that set all of the defined devices.
func (c *Container) DeviceOptions() string {
	var buffer bytes.Buffer
	if c.HasDevices() {
		for _, d := range c.Devices {
			buffer.WriteString(fmt.Sprintf("--device=%s:%s ", d.HostPath, d.ContainerPath))
		}
	}
	return strings.TrimSpace(buffer.String())
}

// VolumesFromOptions returns a string containing the docker command-line options
// that set all of the defined volumes-from.
func (c *Container) VolumesFromOptions(prefix string) string {
	var buffer bytes.Buffer
	if c.HasVolumesFrom() {
		for _, vf := range c.VolumesFrom {
			buffer.WriteString(fmt.Sprintf("--volumes-from=%s-%s ", prefix, vf.NamePrefix))
		}
	}
	return strings.TrimSpace(buffer.String())
}

// StepComponent is where the settings for a tool in a job step are located.
type StepComponent struct {
	Container   Container `json:"container"`
	Type        string    `json:"type"`
	Name        string    `json:"name"`
	Location    string    `json:"location"`
	Description string    `json:"description"`
}

// StepParam is where the params for a step are located.
type StepParam struct {
	ID    string `json:"id"`
	Name  string `json:"name"`
	Value string `json:"value"`
	Order int    `json:"order"`
}

// StepEnvironment defines the environment variables that should be set for a
// step
type StepEnvironment map[string]string

// StepInput describes a single input for a job step.
type StepInput struct {
	ID           string `json:"id"`
	Multiplicity string `json:"multiplicity"`
	Name         string `json:"name"`
	Property     string `json:"property"`
	Retain       bool   `json:"retain"`
	Type         string `json:"type"`
	Value        string `json:"value"`
}

// StepOutput describes a single output for a job step.
type StepOutput struct {
	Multiplicity string `json:"multiplicity"`
	Name         string `json:"name"`
	Property     string `json:"property"`
	QualID       string `json:"qual-id"`
	Retain       bool   `json:"retain"`
	Type         string `json:"type"`
}

// StepConfig is where configuration settings for a job step are located.
type StepConfig struct {
	Params []StepParam  `json:"params"`
	Input  []StepInput  `json:"input"`
	Output []StepOutput `json:"output"`
}

// Step describes a single step in a job. All jobs contain multiple steps.
type Step struct {
	Component   StepComponent
	Config      StepConfig
	Type        string          `json:"type"`
	Stdin       string          `json:"stdin"`
	Stdout      string          `json:"stdout"`
	Stderr      string          `json:"stderr"`
	LogFile     string          `json:"log-file"`
	Environment StepEnvironment `json:"environment"`
	Input       []StepInput     `json:"input"`
	Output      []StepOutput    `json:"output"`
}

// Submission describes a job passed down through the API.
type Submission struct {
	Description        string `json:"description"`
	Email              string `json:"email"`
	Name               string `json:"name"`
	Username           string `json:"username"`
	UUID               string `json:"uuid"`
	AppID              string `json:"app_id"`
	NowDate            string `json:"now_date"`
	RunOnNFS           bool   `json:"run-on-nfs"`
	Type               string `json:"type"`
	NFSBase            string `json:"nfs_base"`
	IRODSBase          string `json:"irods_base"`
	SubmissionDate     string `json:"submission_date"`
	CreateOutputSubdir bool   `json:"create_output_subdir"`
	OutputDir          string `json:"output_dir"` //the value parsed out of the JSON. Use OutputDirectory() instead.
	Steps              []Step `json:"steps"`
	RequestType        string `json:"request_type"`
	AppDescription     string `json:"app_description"`
	WikiURL            string `json:"wiki_url"`
	Notify             bool   `json:"notify"`
	ExecutionTarget    string `json:"execution_target"`
	AppName            string `json:"app_name"`
}

var (
	nowfmt    = "2006-01-02-15-04-05.000"                       // appears in file and directory names.
	validName = regexp.MustCompile(`-\d{4}(?:-\d{2}){5}\.\d+$`) // this isn't included in the Dirname() function so it isn't re-evaluated a lot
	quoteStr  = regexp.MustCompile(`^''|''$`)
	cfg       *configurate.Configuration
	logger    *log.Logger
)

// Init intializes the package. Call this first.
func Init(c *configurate.Configuration, l *log.Logger) {
	cfg = c
	logger = l
}

// New returns a pointer to a newly instantiated Submission with NowDate set.
func New() *Submission {
	n := time.Now().Format(nowfmt)
	return &Submission{
		NowDate: n,
	}
}

// NewFromData creates a new submission and populates it by parsing the passed
// in []byte as JSON.
func NewFromData(data []byte) (*Submission, error) {
	n := time.Now().Format(nowfmt)
	s := &Submission{
		NowDate:        n,
		SubmissionDate: n,
		RunOnNFS:       cfg.RunOnNFS,
		NFSBase:        cfg.NFSBase,
		IRODSBase:      cfg.IRODSBase,
	}
	err := json.Unmarshal(data, s)
	if err != nil {
		return nil, err
	}
	s.Sanitize()
	return s, err
}

// sanitize replaces @ and spaces with _, making a string safe to use as a
// part of a path. Mostly to keep things from getting really confusing when
// a path is passed to Condor.
func sanitize(s string) string {
	step := strings.Replace(s, "@", "_", -1)
	step = strings.Replace(step, " ", "_", -1)
	return step
}

// naivelyquote single-quotes a string that will be placed on the command line
// using plain string substitution.  This works, but may leave extra pairs
// of leading or trailing quotes if there was a leading or trailing quote
// in the original string, which is valid, but may be confusing to human
// readers.
func naivelyquote(s string) string {
	return fmt.Sprintf("'%s'", strings.Replace(s, "'", "''", -1))
}

// quote quotes and escapes a string that is supposed to be passed in to a tool on
// the command line.
func quote(s string) string {
	return quoteStr.ReplaceAllString(naivelyquote(s), "")
}

// Sanitize makes sure the fields in a submission are ready to be used in things
// like file names.
func (s *Submission) Sanitize() {
	s.Username = sanitize(s.Username)
	if s.Type == "" {
		s.Type = "analysis"
	}
	s.Name = sanitize(s.Name)
}

// DirectoryName creates a directory name for an analysis. Used when the submission
// doesn't specify an output directory.  Some types of jobs, for example
// Foundational API jobs, include a timestamp in the job name, so a timestamp
// will not be appended to the directory name in those cases.
func (s *Submission) DirectoryName() string {
	if validName.MatchString(s.Name) {
		return s.Name
	}
	return fmt.Sprintf("%s-%s", s.Name, s.NowDate)
}

// WorkingDirectory returns the path to the working directory for an analysis. This
// value is computed based on values inside the submission, which is why it
// isn't a field in the Submission struct.
func (s *Submission) WorkingDirectory() string {
	return fmt.Sprintf("%s/", path.Join(s.NFSBase, s.Username, s.DirectoryName()))
}

// CondorLogDirectory returns the path to the directory containing condor logs on the
// submission node. This a computed value, so it isn't in the struct.
func (s *Submission) CondorLogDirectory() string {
	return fmt.Sprintf("%s/", path.Join(cfg.CondorLogPath, s.Username, s.DirectoryName()))
}

// IRODSConfig returns the path to iRODS config inside the working directory.
func (s *Submission) IRODSConfig() string {
	return path.Join(s.WorkingDirectory(), "logs", "irods-config")
}

// OutputDirectory returns the path to the output directory in iRODS. It's
// computed, which is why it isn't in the struct. Use this instead of directly
// accessing the OutputDir field.
func (s *Submission) OutputDirectory() string {
	if s.OutputDir == "" {
		return path.Join(s.IRODSBase, s.Username, "analyses", s.DirectoryName())
	} else if s.OutputDir != "" && s.CreateOutputSubdir {
		return path.Join(s.OutputDir, s.DirectoryName())
	} else if s.OutputDir != "" && !s.CreateOutputSubdir {
		return strings.TrimSuffix(s.OutputDir, "/")
	}
	//probably won't ever reach this, but just in case...
	return path.Join(s.IRODSBase, s.Username, "analyses", s.DirectoryName())
}

// DataContainers returns a list of VolumesFrom that describe the data
// containers associated with the job submission.
func (s *Submission) DataContainers() []VolumesFrom {
	var vfs []VolumesFrom
	for _, step := range s.Steps {
		for _, vf := range step.Component.Container.VolumesFrom {
			vfs = append(vfs, vf)
		}
	}
	return vfs
}
