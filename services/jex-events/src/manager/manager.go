package manager

import (
	"api"
	"configurate"
	"fmt"
	"log"
	"logcabin"
	"messaging"
	"os"
	"regexp"
	"strconv"
	"strings"

	"github.com/streadway/amqp"
)

var logger = logcabin.New()

// MsgHandler functions will accept msgs from a Delivery channel and report
// error on the error channel.
type MsgHandler func(<-chan amqp.Delivery, <-chan int, *Databaser, string, string)

// Event contains an event received from the AMQP broker and parsed from JSON.
type Event struct {
	Event        string
	Hash         string
	EventNumber  string
	ID           string
	CondorID     string
	AppID        string
	InvocationID string
	User         string
	Description  string
	EventName    string
	ExitCode     int
	Date         string
	Time         string
	Msg          string
}

func (e *Event) String() string {
	retval := fmt.Sprintf("EventNumber: %s\tID: %s\tCondorID: %s\tDate: %s\tTime: %s\tSHA256: %s\tMsg: %s",
		e.EventNumber,
		e.ID,
		e.CondorID,
		e.Date,
		e.Time,
		e.Hash,
		e.Msg,
	)
	return retval
}

// IsFailure returns true if the event denotes a failed job.
func (e *Event) IsFailure() bool {
	switch e.EventNumber {
	case "002":
		return true
	case "004":
		return true
	case "009":
		return true
	case "010":
		return true
	case "012":
		return true
	case "005": //Assume that Parse() has already been called.
		return e.ExitCode != 0
	default:
		return false
	}
}

const (
	// EventCodeNotSet is the default exit code.
	EventCodeNotSet = -9000
)

// setExitCode will parse out the exit code from the event text, if it's there.
func (e *Event) setExitCode() {
	r := regexp.MustCompile(`\(return value (.*)\)`)
	matches := r.FindStringSubmatch(e.Event)
	matchesLength := len(matches)
	if matchesLength < 2 {
		e.ExitCode = EventCodeNotSet
	}
	code, err := strconv.Atoi(matches[1])
	if err != nil {
		logger.Printf("Error converting exit code to an integer: %s", matches[1])
		e.ExitCode = EventCodeNotSet
	}
	e.ExitCode = code
}

// setCondorID will return the condor ID in the string that's passed in.
func (e *Event) setCondorID() {
	r := regexp.MustCompile(`\(([0-9]+)\.[0-9]+\.[0-9]+\)`)
	matches := r.FindStringSubmatch(e.ID)
	matchesLength := len(matches)
	if matchesLength < 2 {
		e.CondorID = ""
	}
	if strings.HasPrefix(matches[1], "0") {
		e.CondorID = strings.TrimLeft(matches[1], "0")
	} else {
		e.CondorID = matches[1]
	}
}

// setInvocationID will make sure the Invocation ID gets set when appropriate.
func (e *Event) setInvocationID() {
	r := regexp.MustCompile(`IpcUuid = \"(.*)\"`)
	matches := r.FindStringSubmatch(e.Event)
	if len(matches) < 2 {
		e.InvocationID = ""
	} else {
		e.InvocationID = matches[1]
	}
	logger.Printf("Parsed out %s as the invocation ID", e.InvocationID)
}

// Parse extracts info from an event string.
func (e *Event) Parse() {
	r := regexp.MustCompile("^([0-9]{3}) (\\([0-9]+(?:\\.[0-9]+){2}\\)) ([0-9/]+) ([0-9:]+) (.*)\\n")
	matches := r.FindStringSubmatch(e.Event)
	matchesLength := len(matches)
	if matchesLength >= 2 {
		e.EventNumber = matches[1]
	}
	if matchesLength >= 3 {
		e.ID = matches[2]
	}
	if matchesLength >= 4 {
		e.Date = matches[3]
	}
	if matchesLength >= 5 {
		e.Time = matches[4]
	}
	if matchesLength >= 6 {
		e.Msg = matches[5]
	}
	if e.ID != "" {
		e.setCondorID()
	}
	if e.EventNumber == "005" { //This means that the job is in the Completed state.
		e.setExitCode()
	}
	if e.EventNumber == "028" { //parse out execution id from the body of the event.
		e.setInvocationID()
	}
}

// Run puts jex-events in 'events' mode where it listens for events
// on an AMQP exchange, places them into a database, and provides an HTTP
// API on top.
func Run() {
	logger.Println("Configuring database connection...")
	messaging.Init(logger)
	uri, err := configurate.C.String("manager.db_uri")
	if err != nil {
		logger.Fatal(err)
	}
	eventURL, err := configurate.C.String("manager.event_url")
	if err != nil {
		logger.Fatal(err)
	}
	jexURL, err := configurate.C.String("manager.condor_mode_url")
	if err != nil {
		logger.Fatal(err)
	}
	databaser, err := NewDatabaser(uri)
	if err != nil {
		logger.Print(err)
		os.Exit(-1)
	}
	logger.Println("Done configuring database connection.")
	muri, err := configurate.C.String("amqp.uri")
	if err != nil {
		log.Fatal(err)
	}
	client := messaging.NewClient(muri)
	defer client.Close()

	// Setup publishing
	client.SetupPublishing(api.JobsExchange)

	// Accept messages from jobs.updates
	h := PostEventHandler{
		PostURL: eventURL,
		JEXURL:  jexURL,
		DB:      databaser,
	}
	client.AddConsumer(api.JobsExchange, "manager", api.UpdatesKey, h.HandleMessage)

	// Accept messages from jobs.commands
	ch := NewCommandsHandler(client, databaser)
	client.AddConsumer(api.JobsExchange, "manager", api.CommandsKey, ch.Handle)

	// Accept messages from jobs.stops.*
	sh := NewStopsHandler(databaser)
	stopsKey := fmt.Sprintf("%s.*", api.StopsKey)
	client.AddConsumer(api.JobsExchange, "manager", stopsKey, sh.Handle)

	client.Listen()
}
