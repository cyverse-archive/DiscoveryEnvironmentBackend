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

// FileMetadata describes a unit of metadata that should get associated with
// all of the files associated with the job submission.
type FileMetadata struct {
	Attribute string `json:"attr"`
	Value     string `json:"value"`
	Unit      string `json:"unit"`
}

// Argument returns a string containing the command-line settings for the
// file transfer tool.
func (m *FileMetadata) Argument() string {
	return fmt.Sprintf("-m '%s,%s,%s'", m.Attribute, m.Value, m.Unit)
}

// MetadataArgs is a list of FileMetadata
type MetadataArgs []FileMetadata

// FileMetadataArguments returns a string containing the command-line arguments
// for porklock that sets all of the metadata triples.
func (m MetadataArgs) FileMetadataArguments() string {
	var buffer bytes.Buffer
	for _, fm := range m {
		buffer.WriteString(fm.Argument())
		buffer.WriteString(" ")
	}
	return strings.TrimSpace(buffer.String())
}

// Submission describes a job passed down through the API.
type Submission struct {
	Description        string         `json:"description"`
	ArchiveLogs        bool           `json:"archive_logs"`
	Email              string         `json:"email"`
	Name               string         `json:"name"`
	Username           string         `json:"username"`
	UUID               string         `json:"uuid"`
	AppID              string         `json:"app_id"`
	NowDate            string         `json:"now_date"`
	RunOnNFS           bool           `json:"run-on-nfs"`
	Type               string         `json:"type"`
	NFSBase            string         `json:"nfs_base"`
	IRODSBase          string         `json:"irods_base"`
	SkipParentMetadata bool           `json:"skip-parent-meta"`
	SubmissionDate     string         `json:"submission_date"`
	FileMetadata       []FileMetadata `json:"file-metadata"`
	CreateOutputSubdir bool           `json:"create_output_subdir"`
	OutputDir          string         `json:"output_dir"` //the value parsed out of the JSON. Use OutputDirectory() instead.
	Steps              []Step         `json:"steps"`
	RequestType        string         `json:"request_type"`
	AppDescription     string         `json:"app_description"`
	WikiURL            string         `json:"wiki_url"`
	Notify             bool           `json:"notify"`
	ExecutionTarget    string         `json:"execution_target"`
	AppName            string         `json:"app_name"`
	RequestDisk        string         `json:"request_disk"` //untested for now
	Group              string         `json:"group"`        //untested for now
}

// New returns a pointer to a newly instantiated Submission with NowDate set.
func New() *Submission {
	n := time.Now().Format(nowfmt)
	return &Submission{
		NowDate:     n,
		ArchiveLogs: true,
		RequestDisk: cfg.RequestDisk,
	}
}

// NewFromData creates a new submission and populates it by parsing the passed
// in []byte as JSON.
func NewFromData(data []byte) (*Submission, error) {
	s := New()
	s.SubmissionDate = s.NowDate
	s.RunOnNFS = cfg.RunOnNFS
	s.NFSBase = cfg.NFSBase
	s.IRODSBase = cfg.IRODSBase
	err := json.Unmarshal(data, s)
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

// ContainerImages returns a []ContainerImage of all of the images associated
// with this submission.
func (s *Submission) ContainerImages() []ContainerImage {
	var ci []ContainerImage
	for _, step := range s.Steps {
		ci = append(ci, step.Component.Container.Image)
	}
	return ci
}

// Inputs returns all of the StepInputs associated with the submission,
// regardless of what step they're associated with.
func (s *Submission) Inputs() []StepInput {
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
func (s *Submission) Outputs() []StepOutput {
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
func (s *Submission) ExcludeArguments() string {
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
	for _, filter := range strings.Split(cfg.FilterFiles, ",") {
		paths = append(paths, filter)
	}
	if !s.ArchiveLogs {
		paths = append(paths, "logs")
	}
	if len(paths) > 0 {
		return fmt.Sprintf("--exclude %s", strings.Join(paths, ","))
	}
	return ""
}

// AddRequiredMetadata adds any required AVUs that are required but are missing
// from Submission.FileMetadata. This should be called after both of the New*()
// functions and after the Submission has been initialized from JSON.
func (s *Submission) AddRequiredMetadata() {
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
				Value:     s.UUID,
				Unit:      "UUID",
			},
		)
	}
}

// FinalOutputArguments returns a string containing the arguments passed to
// porklock for the final output operation, which transfers all files back into
// iRODS.
func (s *Submission) FinalOutputArguments() string {
	tmpl := "run --rm -v $(pwd):/de-app-work -w /de-app-work discoenv/porklock:%s put --user %s --config irods-config --destination %s %s %s"
	username := s.Username
	dest := quote(s.OutputDirectory())
	metadataArgs := MetadataArgs(s.FileMetadata).FileMetadataArguments()
	excludeArgs := s.ExcludeArguments()
	args := fmt.Sprintf(tmpl, cfg.PorklockTag, username, dest, metadataArgs, excludeArgs)
	if s.SkipParentMetadata {
		args = fmt.Sprintf("%s --skip-parent-meta", args)
	}
	return args
}
