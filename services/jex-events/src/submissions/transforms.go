package submissions

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
	ID           string         `json:"id"`
	Volumes      []Volume       `json:"container_volumes"`
	Devices      []Device       `json:"container_devices"`
	VolumesFroms []VolumesFrom  `json:"container_volumes_from"`
	Name         string         `json:"name"`
	NetworkMode  string         `json:"network_mode"`
	CPUShares    string         `json:"cpu_shares"`
	MemoryLimit  string         `json:"memory_limit"`
	Image        ContainerImage `json:"image"`
	EntryPoint   string         `json:"entrypoint"`
}

// DataContainer describes a container that is used by at least one step in an
// analysis. Has the same format as a VolumesFrom.
type DataContainer struct {
	VolumesFrom
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
	Order string `json:"order"`
}

// StepConfig is where configuration settings for a job step are located.
type StepConfig struct {
	Params []StepParam `json:"params"`
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
var Submission struct {
	Description        string          `json:"description"`
	Email              string          `json:"email"`
	Name               string          `json:"name"`
	Username           string          `json:"username"`
	UUID               string          `json:"uuid"`
	AppID              string          `json:"app_id"`
	NowDate            string          `json:"now_date"`
	RunOnNFS           bool            `json:"run-on-nfs"`
	Type               string          `json:"type"`
	NFSBase            string          `json:"nfs_base"`
	IRODSBase          string          `json:"irods_base"`
	SubmissionDate     string          `json:"submission_date"`
	CreateOutputSubdir bool            `json:"create_output_subdir"`
	CondorLogDir       string          `json:"condor-log-dir"`
	WorkingDir         string          `json:"working_dir"`
	OutputDir          string          `json:"output_dir"`
	DataContainers     []DataContainer `json:"data_containers"`
	Steps              []Step          `json:"steps"`
	RequestType        string          `json:"request_type"`
	AppDescription     string          `json:"app_description"`
	WikiURL            string          `json:"wiki_url"`
	Notify             bool            `json:"notify"`
	ExecutionTarget    string          `json:"execution_target"`
	AppName            string          `json:"app_name"`
}

var (
	nowfmt = "2006-01-02-15-04-05.000" // appears in file and directory names.
)
