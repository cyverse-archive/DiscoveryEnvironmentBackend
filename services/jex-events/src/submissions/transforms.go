package submissions

import (
	"bytes"
	"configurate"
	"encoding/json"
	"fmt"
	"log"
	"path"
	"regexp"
	"sort"
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

// NameOption returns a string containing the docker command-line option
// that sets the container name.
func (c *Container) NameOption() string {
	if c.Name != "" {
		return fmt.Sprintf("--name %s", c.Name)
	}
	return ""
}

// NetworkModeOption returns a string containing the docker command-line option
// that sets the container network mode.
func (c *Container) NetworkModeOption() string {
	if c.NetworkMode != "" {
		return fmt.Sprintf("--net=%s", c.NetworkMode)
	}
	return "--net==bridge"
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
	if c.IsDEImage() {
		return fmt.Sprintf(":%s", cfg.PorklockTag)
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

// ByOrder implements the sort interface for a []StepParam based on the Order
// field.
type ByOrder []StepParam

// Len returns the number of elements in a ByOrder
func (o ByOrder) Len() int {
	return len(o)
}

// Swap swaps two positions in a []StepParam
func (o ByOrder) Swap(i, j int) {
	o[i], o[j] = o[j], o[i]
}

// Less returns true if position i is less than position j.
func (o ByOrder) Less(i, j int) bool {
	return o[i].Order < o[j].Order
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

// IRODSPath returns a string containing the iRODS path to an input file.
func (i *StepInput) IRODSPath() string {
	if i.Multiplicity == "collection" {
		if !strings.HasSuffix(i.Value, "/") {
			return fmt.Sprintf("%s/", i.Value)
		}
	}
	return i.Value
}

// LogFilename returns a string containing the input job's log filename in the
// format "log-input-<suffix>"
func (i *StepInput) LogFilename(suffix string) string {
	return fmt.Sprintf("input-%s", suffix)
}

// Stdout returns a string containing the path to the input job's stdout file.
// It should be a relative path in the format "logs/logs-stdout-<LogFilename(suffix)>"
func (i *StepInput) Stdout(suffix string) string {
	return path.Join("logs", fmt.Sprintf("logs-stdout-%s", i.LogFilename(suffix)))
}

// Stderr returns a string containing the path to the input job's stderr file.
// It should be a relative path in the format "logs/logs-stderr-<LogFilename(suffix)>"
func (i *StepInput) Stderr(suffix string) string {
	return path.Join("logs", fmt.Sprintf("logs-stderr-%s", i.LogFilename(suffix)))
}

// LogPath returns the path to the Condor log file for the input job. The returned
// path will be in the format "<parent>/logs/logs-condor-<LogFilename(suffix)>"
func (i *StepInput) LogPath(parent, suffix string) string {
	return path.Join(parent, "logs", fmt.Sprintf("logs-condor-%s", i.LogFilename(suffix)))
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

// Parameters returns the StepParams associated with a Step in the correct order.
// Use this to get the list of Params rather than accessing the field directory.
func (c *StepConfig) Parameters() []StepParam {
	sort.Sort(ByOrder(c.Params))
	for _, p := range c.Params {
		p.Value = quote(p.Value)
	}
	return c.Params
}

// Step describes a single step in a job. All jobs contain multiple steps.
type Step struct {
	Component   StepComponent
	Config      StepConfig
	Type        string          `json:"type"`
	StdinPath   string          `json:"stdin"`
	StdoutPath  string          `json:"stdout"`
	StderrPath  string          `json:"stderr"`
	LogFile     string          `json:"log-file"`
	Environment StepEnvironment `json:"environment"`
	Input       []StepInput     `json:"input"`
	Output      []StepOutput    `json:"output"`
}

// EnvOptions returns a string containing the docker command-line options
// that set the environment variables listed in the Environment field.
func (s *Step) EnvOptions() string {
	var buffer bytes.Buffer
	for k, v := range s.Environment {
		buffer.WriteString(fmt.Sprintf("--env=\"%s=%s\" ", k, v))
	}
	return strings.TrimSpace(buffer.String())
}

// IsBackwardsCompatible returns true if the job submission uses the container
// image(s) put together to maintain compatibility with non-dockerized versions
// of the DE.
func (s *Step) IsBackwardsCompatible() bool {
	img := s.Component.Container.Image.Name
	return strings.HasPrefix(img, "discoenv/backwards-compat") ||
		strings.HasPrefix(img, "gims.iplantcollaborative.org:5000/backwards-compat")
}

// BackwardsCompatibleOptions returns a string with the options that are needed
// for the image that provides backwards compatibility with pre-Docker tools.
func (s *Step) BackwardsCompatibleOptions() string {
	if s.IsBackwardsCompatible() {
		return "-v /usr/local2/:/usr/local2 -v /usr/local3/:/usr/local3/ -v /data2/:/data2/"
	}
	return ""
}

// Executable returns a string containing the executable path as it gets placed
// inside the docker command-line.
func (s *Step) Executable() string {
	if s.IsBackwardsCompatible() {
		return path.Join(s.Component.Location, s.Component.Name)
	}
	return ""
}

// CommandLine returns a string containing all of the options passed to the
// docker run command for this step in the submission.
func (s *Step) CommandLine(uuid string) string {
	container := s.Component.Container
	allLines := []string{
		"run --rm -e IPLANT_USER -e IPLANT_EXECUTION_ID",
		s.BackwardsCompatibleOptions(),
		container.VolumeOptions(),
		container.DeviceOptions(),
		container.VolumesFromOptions(uuid),
		container.NameOption(),
		container.WorkingDirectoryOption(),
		container.MemoryLimitOption(),
		container.CPUSharesOption(),
		container.NetworkModeOption(),
		s.EnvOptions(),
		container.EntryPointOption(),
		container.ImageOption(),
		s.Executable(),
	}
	var cmdLine []string
	for _, l := range allLines {
		if l != "" {
			cmdLine = append(cmdLine, l)
		}
	}
	return strings.Join(cmdLine, " ")
}

// Arguments returns a string that contains all of the fields that go into the
// command in the iplant.sh file. Combines the output of CommandLine() with
// the formatted params from the Config.
func (s *Step) Arguments(uuid string) string {
	var buffer bytes.Buffer
	for _, p := range s.Config.Parameters() {
		buffer.WriteString(fmt.Sprintf("%s %s ", p.Name, p.Value))
	}
	return strings.TrimSpace(fmt.Sprintf("%s %s", s.CommandLine(uuid), buffer.String()))
}

// Stdin returns the a quoted version of s.StdinPath or an empty string if it's
// not set.
func (s *Step) Stdin() string {
	if s.StdinPath != "" {
		return quote(s.StdinPath)
	}
	return s.StdinPath
}

// Stdout returns the quoted version of s.StdoutPath or a default value located in
// the logs directory of the working directory. 'suffix' is appended to the
// filename in the logs directory, but only if s.StdoutPath isn't set.
func (s *Step) Stdout(suffix string) string {
	if s.StdoutPath != "" {
		return quote(s.StdoutPath)
	}
	return path.Join("logs", fmt.Sprintf("%s%s", "condor-stdout-", suffix))
}

// Stderr returns the quoted version of s.StderrPath or a default value located in
// the logs directory of the working directory. 'suffix' is appended to the
// filename in the logs directory, but only if s.StderrPath isn't set.
func (s *Step) Stderr(suffix string) string {
	if s.StderrPath != "" {
		return quote(s.StderrPath)
	}
	return path.Join("logs", fmt.Sprintf("%s%s", "condor-stderr-", suffix))
}

// LogPath uses the value of step.LogFile and params to generate a path to a
// log file. If Step.LogFile isn't empty, it's placed inside the directory
// specified by parent. If it is empty, a path like
// "<parent>/logs/condor-log-<suffix>" is returned.
func (s *Step) LogPath(parent, suffix string) string {
	if s.LogFile != "" {
		return path.Join(parent, s.LogFile)
	}
	return path.Join(parent, "logs", fmt.Sprintf("condor-log-%s", suffix))
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
