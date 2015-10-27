package model

import (
	"bytes"
	"fmt"
	"path"
	"sort"
	"strings"
)

// StepComponent is where the settings for a tool in a job step are located.
type StepComponent struct {
	Container   Container `json:"container"`
	Type        string    `json:"type"`
	Name        string    `json:"name"`
	Location    string    `json:"location"`
	Description string    `json:"description"`
}

// StepEnvironment defines the environment variables that should be set for a
// step
type StepEnvironment map[string]string

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
func (s *Step) EnvOptions() []string {
	retval := []string{}
	for k, v := range s.Environment {
		retval = append(retval, fmt.Sprintf("--env=\"%s=%s\"", k, v))
	}
	return retval
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
func (s *Step) BackwardsCompatibleOptions() []string {
	if s.IsBackwardsCompatible() {
		return []string{"-v", "/usr/local2/:/usr/local2", "-v", "/usr/local3/:/usr/local3/", "-v", "/data2/:/data2/"}
	}
	return []string{}
}

// Executable returns a string containing the executable path as it gets placed
// inside the docker command-line.
func (s *Step) Executable() string {
	if s.IsBackwardsCompatible() {
		return path.Join(s.Component.Location, s.Component.Name)
	}
	return ""
}

// Arguments returns a []string containing all of the options passed to the
// docker run command for this step in the submission.
func (s *Step) Arguments() []string {
	allLines := []string{s.Executable()}
	for _, p := range s.Config.Parameters() {
		if p.Name != "" {
			allLines = append(allLines, p.Name)
		}
		if p.Value != "" {
			allLines = append(allLines, p.Value)
		}
	}
	var cmdLine []string
	for _, l := range allLines {
		if l != "" {
			cmdLine = append(cmdLine, l)
		}
	}
	return cmdLine
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
		return s.StdoutPath
	}
	return path.Join("logs", fmt.Sprintf("%s%s", "condor-stdout-", suffix))
}

// Stderr returns the quoted version of s.StderrPath or a default value located in
// the logs directory of the working directory. 'suffix' is appended to the
// filename in the logs directory, but only if s.StderrPath isn't set.
func (s *Step) Stderr(suffix string) string {
	if s.StderrPath != "" {
		return s.StderrPath
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

// StepConfig is where configuration settings for a job step are located.
type StepConfig struct {
	Params  []StepParam  `json:"params"`
	Inputs  []StepInput  `json:"input"`
	Outputs []StepOutput `json:"output"`
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

// PreviewableStepParam is a list of StepParams that can be returned as a string
// that previews the command-line for a submission.
type PreviewableStepParam []StepParam

func (p PreviewableStepParam) String() string {
	var buffer bytes.Buffer
	sort.Sort(ByOrder(p))
	for _, param := range p {
		buffer.WriteString(fmt.Sprintf("%s %s ", param.Name, quote(param.Value)))
	}
	return strings.TrimSpace(buffer.String())
}
