package events

import (
	"configurate"
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"messaging"
	"os"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/streadway/amqp"
)

var logger *log.Logger

// MsgHandler functions will accept msgs from a Delivery channel and report
// error on the error channel.
type MsgHandler func(<-chan amqp.Delivery, <-chan int, *Databaser, string, string)

// reconnect is a handler for AMQP errors.
func reconnect(errorChan chan messaging.ConnectionError) {
	msg := <-errorChan
	exitChan := msg.Channel

	for {
		select {
		case exitError, ok := <-exitChan:
			if !ok {
				logger.Println("Exit channel closed.")
			}
			logger.Println(exitError)
			logger.Println("An error was detected with the AMQP connection. Exiting with a -1000 exit code.")
			os.Exit(-1000)
		}
	}
}

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

// EventHandler processes incoming event messages
func EventHandler(deliveries <-chan amqp.Delivery, d *Databaser, postURL string, JEXURL string) {
	eventHandler := PostEventHandler{
		PostURL: postURL,
		JEXURL:  JEXURL,
		DB:      d,
	}
	for {
		select {
		case delivery := <-deliveries:
			body := delivery.Body
			delivery.Ack(false) //We're not doing batch deliveries, which is what the false means
			var event Event
			err := json.Unmarshal(body, &event)
			if err != nil {
				logger.Print(err)
				logger.Print(string(body[:]))
				continue
			}

			// parse the body of the event that came from the amqp broker.
			event.Parse()
			logger.Println(event.String())

			// adds the job to the database, but only if it doesn't already exist.
			job, err := d.AddJob(event.CondorID)
			if err != nil {
				logger.Printf("Error adding job: %s", err)
				continue
			}

			// make sure the exit code is set so that it gets updated in upcoming steps.
			job.ExitCode = event.ExitCode

			// set the invocation id, but only if it's not set and the event actually
			// has a value to update it with.
			if job.InvocationID == "" && event.InvocationID != "" {
				job.InvocationID = event.InvocationID
				logger.Printf("Setting InvocationID to %s", job.InvocationID)
			} else {
				logger.Printf("Setting the InvocationID was not necessary")
			}

			// we're expecting an exit code of 0 for successful runs. HT jobs may have
			// more than one failure.
			if job.ExitCode != 0 {
				job.FailureCount = job.FailureCount + 1
			}

			// update the job with any additional information
			job, err = d.UpdateJob(job) // Need to make sure the exit code gets stored.
			if err != nil {
				logger.Printf("Error updating job")
			}

			// update the parsed event object with info returned from the database.
			event.CondorID = job.CondorID
			event.InvocationID = job.InvocationID
			event.AppID = job.AppID
			event.User = job.Submitter

			// don't add the event to the database if it's already there.
			exists, err := d.DoesCondorJobEventExist(event.Hash)
			if err != nil {
				logger.Printf("Error checking for job event existence by checksum: %s", event.Hash)
				continue
			}
			if exists {
				logger.Printf("An event with a hash of %s already exists in the database, skipping", event.Hash)
				continue
			}

			// send event updates upstream to Donkey
			err = eventHandler.Route(&event)
			if err != nil {
				logger.Printf("Error sending event upstream: %s", err)
			}

			// store the unparsed (raw), event information in the database.
			rawEventID, err := d.AddCondorRawEvent(event.Event, job.ID)
			if err != nil {
				logger.Printf("Error adding raw event: %s", err)
				continue
			}
			ce, err := d.GetCondorEventByNumber(event.EventNumber)
			if err != nil {
				logger.Printf("Error getting condor event: %s", err)
				continue
			}
			jobEventID, err := d.AddCondorJobEvent(job.ID, ce.ID, rawEventID, event.Hash)
			if err != nil {
				logger.Printf("Error adding job event: %s", err)
				continue
			}
			if eventHandler.ShouldUpdateLastEvents(&event) {
				_, err = d.UpsertLastCondorJobEvent(jobEventID, job.ID)
				if err != nil {
					logger.Printf("Error upserting last condor job event: %s", err)
					continue
				}
			}
		}
	}
}

// Run puts jex-events in 'events' mode where it listens for events
// on an AMQP exchange, places them into a database, and provides an HTTP
// API on top.
func Run(config *configurate.Configuration, l *log.Logger) {
	logger = l
	logger.Println("Configuring database connection...")
	messaging.Init(logger)
	databaser, err := NewDatabaser(config.DBURI)
	if err != nil {
		logger.Print(err)
		os.Exit(-1)
	}
	logger.Println("Done configuring database connection.")

	connErrChan := make(chan messaging.ConnectionError)
	consumer := messaging.NewAMQPConsumer(config)
	messaging.SetupReconnection(connErrChan, reconnect)
	logger.Print("Setting up HTTP")
	SetupHTTP(config, databaser)
	logger.Print("Done setting up HTTP")

	// This is the retry logic that the events mode goes through at start up. It's
	// there just in case the AMQP broker isn't up when events mode starts.
	randomizer := rand.New(rand.NewSource(time.Now().UnixNano()))
	var deliveries <-chan amqp.Delivery
	for {
		logger.Println("Attempting AMQP connection...")
		deliveries, err = consumer.Connect(connErrChan)
		if err != nil {
			logger.Print(err)
			waitFor := randomizer.Intn(10)
			logger.Printf("Re-attempting connection in %d seconds", waitFor)
			time.Sleep(time.Duration(waitFor) * time.Second)
		} else {
			logger.Println("Successfully connected to the AMQP broker")
			break
		}
	}

	// The actual logic for events mode occurs here.
	EventHandler(deliveries, databaser, config.EventURL, config.JEXURL)
}
