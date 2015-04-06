package main

import (
	"bytes"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"os/exec"
)

var (
	cmdsFile  = flag.String("cmds", "", "Path to JSON encoded file containing commands to run.")
	statusURL = flag.String("status", "", "URL that statuses should be POSTed to.")
)

func init() {
	flag.Parse()
}

// Command is a type that encapsulates the information needed to run a command.
type Command struct {
	Env    []string `json:"env"`
	Cmd    []string `json:"cmd"`
	Stdout string   `json:"stdout"`
	Stderr string   `json:"stderr"`
	Stdin  string   `json:"stdin"`
}

// Validate returns true if the Command is runnable and false if it isn't.
func (c *Command) Validate() bool {
	valid := true
	if c.Cmd == nil {
		fmt.Println("cmd not set")
		valid = false
	}
	if c.Stdout == "" {
		fmt.Println("stdout not set")
		valid = false
	}
	if c.Stderr == "" {
		fmt.Println("stderr not set")
		valid = false
	}
	return valid
}

// Execute runs the Command and returns any errors that occur.
func (c *Command) Execute() error {
	stdoutWriter, err := os.Create(c.Stdout)
	if err != nil {
		return err
	}
	stderrWriter, err := os.Create(c.Stderr)
	if err != nil {
		return err
	}
	//Reading from stdin is optional
	var stdinReader *os.File
	if c.Stdin != "" {
		stdinReader, err = os.Open(c.Stdin)
		if err != nil {
			return err
		}
	} else {
		stdinReader = nil
	}
	cmd := exec.Command(c.Cmd[0], c.Cmd[1:]...)
	if cmd.Env != nil {
		cmd.Env = c.Env
	}
	//Again, remember that reading from stdin is optional
	if c.Stdin != "" {
		cmd.Stdin = stdinReader
	}
	cmd.Stdout = stdoutWriter
	cmd.Stderr = stderrWriter
	err = cmd.Run()
	return err
}

//Commands contains a list of Command instances.
type Commands struct {
	Commands      []Command `json:"commands"`
	LogsDir       string    `json:"logs_dir"`
	InvocationID  string    `json:"invocation_id"`
	ApplicationID string    `json:"application_id"`
	Submitter     string    `json:"submitter"`
}

// UpdateMsg contains the fields that are sent as updates to the provided endpoint.
type UpdateMsg struct {
	Msg           string `json:"msg"`
	InvocationID  string `json:"invocation_id"`
	ApplicationID string `json:"application_id"`
	Submitter     string `json:"submitter"`
	Status        string `json:"status"`
}

// NewUpdateMsg creates a new UpdateMsg and returns a pointer to it.
func (cl *Commands) NewUpdateMsg(status string, msgString string) *UpdateMsg {
	msg := &UpdateMsg{
		Msg:           msgString,
		InvocationID:  cl.InvocationID,
		ApplicationID: cl.ApplicationID,
		Submitter:     cl.Submitter,
		Status:        status,
	}
	return msg
}

// SendUpdate posts a JSON-encoded UpdateMsg to the configured statusURL.
func (cl *Commands) SendUpdate(status string, msgString string) error {
	msgJSON, err := json.Marshal(cl.NewUpdateMsg(status, msgString))
	if err != nil {
		return err
	}
	resp, err := http.Post(*statusURL, "application/json", bytes.NewReader(msgJSON))
	if err != nil {
		return err
	}
	log.Println(resp)
	return nil
}

// Validate iterates throught the Commands and runs their respective Validate()
// function. If any of them return false, then this Validate() returns false.
func (cl *Commands) Validate() bool {
	valid := true
	var v bool
	for _, c := range cl.Commands {
		v = c.Validate()
		if !v {
			valid = false
		}
	}
	if cl.LogsDir == "" {
		fmt.Println("logs_dir not set")
		valid = false
	}
	if cl.InvocationID == "" {
		fmt.Println("invocation_id not set")
		valid = false
	}
	if cl.ApplicationID == "" {
		fmt.Println("application_id not set")
		valid = false
	}
	if cl.Submitter == "" {
		fmt.Println("submitter not set")
		valid = false
	}
	return valid
}

// Execute runs all of the commands contained in the Commands instance and
// returns an error if any of them error. Otherwise it returns nil. It also
// creates the LogsDir, if defined.
func (cl *Commands) Execute() []error {
	//First, create the LogsDir since each command might depend on it.
	var err error
	var errors []error
	cl.SendUpdate("Running", "Creating logs directory")
	if cl.LogsDir != "" {
		err = os.MkdirAll(cl.LogsDir, 0777)
		if err != nil {
			cl.SendUpdate("Failed", fmt.Sprintf("Failed to create logs directory: %s", err))
			errors = append(errors, err)
			return errors
		}
	}
	cl.SendUpdate("Running", "Done creating logs directory")
	numCmds := len(cl.Commands)
	for n, c := range cl.Commands {
		cl.SendUpdate("Running", fmt.Sprintf("Running command %d of %d", n+1, numCmds))
		err = c.Execute()
		if err != nil {
			cl.SendUpdate("Failed", fmt.Sprintf("Command %d of %d failed. Check logs for the reason.", n+1, numCmds))
			errors = append(errors, err)
		}
	}
	if len(errors) == 0 {
		cl.SendUpdate("Completed", "Analysis complete")
	}
	return errors
}

// ReadCmdsFile reads JSON from 'path' and returns a pointer to a Commands
// instance.
func ReadCmdsFile(path string) (*Commands, error) {
	fileInfo, err := os.Stat(path)
	if err != nil {
		return nil, err
	}
	if fileInfo.IsDir() {
		return nil, fmt.Errorf("%s is a directory", path)
	}
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	fileData, err := ioutil.ReadAll(file)
	if err != nil {
		return nil, err
	}
	var cmds Commands
	err = json.Unmarshal(fileData, &cmds)
	if err != nil {
		return &cmds, err
	}
	return &cmds, nil
}

func main() {
	if *cmdsFile == "" {
		fmt.Println("--cmds must be set.")
		os.Exit(-1)
	}
	cmds, err := ReadCmdsFile(*cmdsFile)
	if err != nil {
		fmt.Printf("Error reading --cmds file: %s", err)
		os.Exit(-1)
	}
	valid := cmds.Validate()
	if !valid {
		fmt.Println("One or more commands were not valid.")
		os.Exit(-1)
	}
	errors := cmds.Execute()
	if len(errors) > 0 {
		for _, e := range errors {
			fmt.Println(e)
		}
		os.Exit(-1)
	}
}
