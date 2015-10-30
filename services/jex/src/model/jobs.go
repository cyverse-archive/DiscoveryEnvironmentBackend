package model

import (
	"configurate"
	"encoding/json"
	"fmt"
	"path"
	"regexp"
	"strings"
	"time"
)

var (
	validName = regexp.MustCompile(`-\d{4}(?:-\d{2}){5}\.\d+$`) // this isn't included in the Dirname() function so it isn't re-evaluated a lot
	quoteStr  = regexp.MustCompile(`^''|''$`)
)

const (
	nowfmt = "2006-01-02-15-04-05.000" // appears in file and directory names.

	//DockerLabelKey is the key for the labels applied to all containers associated with a job.
	DockerLabelKey = "org.iplantc.analysis"
)

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

// ExtractJobID pulls the job id from the given []byte, if it exists. Returns
// an empty []byte if it doesn't.
func ExtractJobID(output []byte) []byte {
	extractor := regexp.MustCompile(`submitted to cluster ((\d+)+)`)
	matches := extractor.FindAllSubmatch(output, -1)
	var thematch []byte
	if len(matches) > 0 {
		if len(matches[0]) > 1 {
			thematch = matches[0][1]
		}
	}
	return thematch
}

// Job is a type that contains info that goes into the jobs table.
type Job struct {
	AppDescription     string         `json:"app_description"`
	AppID              string         `json:"app_id"`
	AppName            string         `json:"app_name"`
	ArchiveLogs        bool           `json:"archive_logs"`
	ID                 string         `json:"id"`
	BatchID            string         `json:"batch_id"`
	CondorID           string         `json:"condor_id"`
	CreateOutputSubdir bool           `json:"create_output_subdir"`
	DateSubmitted      time.Time      `json:"date_submitted"`
	DateStarted        time.Time      `json:"date_started"`
	DateCompleted      time.Time      `json:"date_completed"`
	Description        string         `json:"description"`
	Email              string         `json:"email"`
	ExecutionTarget    string         `json:"execution_target"`
	ExitCode           int            `json:"exit_code"`
	FailureCount       int64          `json:"failure_count"`
	FailureThreshold   int64          `json:"failure_threshold"`
	FileMetadata       []FileMetadata `json:"file-metadata"`
	Group              string         `json:"group"` //untested for now
	InvocationID       string         `json:"uuid"`
	IRODSBase          string         `json:"irods_base"`
	Name               string         `json:"name"`
	NFSBase            string         `json:"nfs_base"`
	Notify             bool           `json:"notify"`
	NowDate            string         `json:"now_date"`
	OutputDir          string         `json:"output_dir"`   //the value parsed out of the JSON. Use OutputDirectory() instead.
	RequestDisk        string         `json:"request_disk"` //untested for now
	RequestType        string         `json:"request_type"`
	RunOnNFS           bool           `json:"run-on-nfs"`
	SkipParentMetadata bool           `json:"skip-parent-meta"`
	Steps              []Step         `json:"steps"`
	SubmissionDate     string         `json:"submission_date"`
	Submitter          string         `json:"username"`
	TimeLimit          int64          `json:"time_limit"`
	Type               string         `json:"type"`
	WikiURL            string         `json:"wiki_url"`
}

// New returns a pointer to a newly instantiated Job with NowDate set.
func New() *Job {
	n := time.Now().Format(nowfmt)
	rq, err := configurate.C.String("condor.request_disk")
	if err != nil {
		rq = ""
	}
	return &Job{
		NowDate:     n,
		ArchiveLogs: true,
		RequestDisk: rq,
		TimeLimit:   3600,
	}
}

// NewFromData creates a new submission and populates it by parsing the passed
// in []byte as JSON.
func NewFromData(data []byte) (*Job, error) {
	var err error
	s := New()
	s.SubmissionDate = s.NowDate
	s.IRODSBase, err = configurate.C.String("irods.base")
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal(data, s)
	if err != nil {
		return nil, err
	}
	s.Sanitize()
	s.AddRequiredMetadata()
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

// Sanitize makes sure the fields in a submission are ready to be used in things
// like file names.
func (s *Job) Sanitize() {
	s.Submitter = sanitize(s.Submitter)
	if s.Type == "" {
		s.Type = "analysis"
	}
	s.Name = sanitize(s.Name)
}

// DirectoryName creates a directory name for an analysis. Used when the submission
// doesn't specify an output directory.  Some types of jobs, for example
// Foundational API jobs, include a timestamp in the job name, so a timestamp
// will not be appended to the directory name in those cases.
func (s *Job) DirectoryName() string {
	if validName.MatchString(s.Name) {
		return s.Name
	}
	return fmt.Sprintf("%s-%s", s.Name, s.NowDate)
}

// CondorLogDirectory returns the path to the directory containing condor logs on the
// submission node. This a computed value, so it isn't in the struct.
func (s *Job) CondorLogDirectory() string {
	logPath, err := configurate.C.String("condor.log_path")
	if err != nil {
		logPath = ""
	}
	return fmt.Sprintf("%s/", path.Join(logPath, s.Submitter, s.DirectoryName()))
}

// IRODSConfig returns the path to iRODS config inside the working directory.
func (s *Job) IRODSConfig() string {
	return path.Join("logs", "irods-config")
}

// OutputDirectory returns the path to the output directory in iRODS. It's
// computed, which is why it isn't in the struct. Use this instead of directly
// accessing the OutputDir field.
func (s *Job) OutputDirectory() string {
	if s.OutputDir == "" {
		return path.Join(s.IRODSBase, s.Submitter, "analyses", s.DirectoryName())
	} else if s.OutputDir != "" && s.CreateOutputSubdir {
		return path.Join(s.OutputDir, s.DirectoryName())
	} else if s.OutputDir != "" && !s.CreateOutputSubdir {
		return strings.TrimSuffix(s.OutputDir, "/")
	}
	//probably won't ever reach this, but just in case...
	return path.Join(s.IRODSBase, s.Submitter, "analyses", s.DirectoryName())
}

// DataContainers returns a list of VolumesFrom that describe the data
// containers associated with the job submission.
func (s *Job) DataContainers() []VolumesFrom {
	var vfs []VolumesFrom
	for _, step := range s.Steps {
		for _, vf := range step.Component.Container.VolumesFrom {
			vfs = append(vfs, vf)
		}
	}
	return vfs
}

// ContainerImages returns a []ContainerImage of all of the images associated
// with this submission.
func (s *Job) ContainerImages() []ContainerImage {
	var ci []ContainerImage
	for _, step := range s.Steps {
		ci = append(ci, step.Component.Container.Image)
	}
	return ci
}

// Inputs returns all of the StepInputs associated with the submission,
// regardless of what step they're associated with.
func (s *Job) Inputs() []StepInput {
	var inputs []StepInput
	for _, step := range s.Steps {
		for _, input := range step.Config.Inputs {
			inputs = append(inputs, input)
		}
	}
	return inputs
}

// Outputs returns all of the StepOutputs associated with the submission,
// regardless of what step they're associated with.
func (s *Job) Outputs() []StepOutput {
	var outputs []StepOutput
	for _, step := range s.Steps {
		for _, output := range step.Config.Outputs {
			outputs = append(outputs, output)
		}
	}
	return outputs
}

// ExcludeArguments returns a string containing the command-line settings for
// porklock that tell it which files to skip.
func (s *Job) ExcludeArguments() []string {
	var paths []string
	for _, input := range s.Inputs() {
		if !input.Retain {
			paths = append(paths, input.Source())
		}
	}
	for _, output := range s.Outputs() {
		if !output.Retain {
			paths = append(paths, output.Source())
		}
	}
	filterFiles, err := configurate.C.String("condor.filter_files")
	if err != nil {
		filterFiles = ""
	}
	for _, filter := range strings.Split(filterFiles, ",") {
		paths = append(paths, filter)
	}
	if !s.ArchiveLogs {
		paths = append(paths, "logs")
	}
	retval := []string{}
	if len(paths) > 0 {
		retval = append(retval, "--exclude")
		retval = append(retval, strings.Join(paths, ","))
	}
	return retval
}

// AddRequiredMetadata adds any required AVUs that are required but are missing
// from Job.FileMetadata. This should be called after both of the New*()
// functions and after the Job has been initialized from JSON.
func (s *Job) AddRequiredMetadata() {
	foundAnalysis := false
	foundExecution := false
	for _, md := range s.FileMetadata {
		if md.Attribute == "ipc-analysis-id" {
			foundAnalysis = true
		}
		if md.Attribute == "ipc-execution-id" {
			foundExecution = true
		}
	}
	if !foundAnalysis {
		s.FileMetadata = append(
			s.FileMetadata,
			FileMetadata{
				Attribute: "ipc-analysis-id",
				Value:     s.AppID,
				Unit:      "UUID",
			},
		)
	}
	if !foundExecution {
		s.FileMetadata = append(
			s.FileMetadata,
			FileMetadata{
				Attribute: "ipc-execution-id",
				Value:     s.InvocationID,
				Unit:      "UUID",
			},
		)
	}
}

// FinalOutputArguments returns a string containing the arguments passed to
// porklock for the final output operation, which transfers all files back into
// iRODS.
func (s *Job) FinalOutputArguments() []string {
	dest := quote(s.OutputDirectory())
	retval := []string{
		"put",
		"--user", s.Submitter,
		"--config", "irods-config",
		"--destination", dest,
	}
	for _, m := range MetadataArgs(s.FileMetadata).FileMetadataArguments() {
		retval = append(retval, m)
	}
	for _, e := range s.ExcludeArguments() {
		retval = append(retval, e)
	}
	if s.SkipParentMetadata {
		retval = append(retval, "--skip-parent-meta")
	}
	return retval
}

// FileMetadata describes a unit of metadata that should get associated with
// all of the files associated with the job submission.
type FileMetadata struct {
	Attribute string `json:"attr"`
	Value     string `json:"value"`
	Unit      string `json:"unit"`
}

// Argument returns a string containing the command-line settings for the
// file transfer tool.
func (m *FileMetadata) Argument() []string {
	return []string{"-m", fmt.Sprintf("'%s,%s,%s'", m.Attribute, m.Value, m.Unit)}
}

// MetadataArgs is a list of FileMetadata
type MetadataArgs []FileMetadata

// FileMetadataArguments returns a string containing the command-line arguments
// for porklock that sets all of the metadata triples.
func (m MetadataArgs) FileMetadataArguments() []string {
	retval := []string{}
	for _, fm := range m {
		for _, a := range fm.Argument() {
			retval = append(retval, a)
		}
	}
	return retval
}

// CondorJobEvent ties a model.CondorEvent to a job and raw event.
type CondorJobEvent struct {
	ID               string
	JobID            string
	CondorEventID    string
	CondorRawEventID string
	Hash             string
	DateTriggered    time.Time
}

// CondorEvent contains info about an event that Condor emitted.
type CondorEvent struct {
	ID          string
	EventNumber string
	EventName   string
	EventDesc   string
}

// CondorRawEvent contains the raw, unparsed event that was emitted from Condor.
type CondorRawEvent struct {
	ID            string
	JobID         string
	EventText     string
	DateTriggered time.Time
}

// CondorJobDep tracks dependencies between jobs.
type CondorJobDep struct {
	SuccessorID   string
	PredecessorID string
}

// CondorJobStopRequest records a request to stop a job.
type CondorJobStopRequest struct {
	ID            string
	JobID         string
	Username      string
	DateRequested time.Time
	Reason        string
}

// LastCondorJobEvent records the last updated CondorJobEvent for a job.
type LastCondorJobEvent struct {
	JobID            string
	CondorJobEventID string
}

// Version contains info about the version of the database in use.
type Version struct {
	ID      int64
	Version string
	Applied time.Time
}
