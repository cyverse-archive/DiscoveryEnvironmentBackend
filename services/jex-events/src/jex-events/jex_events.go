package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"math/rand"
	"os"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/streadway/amqp"
)

var (
	cfgPath = flag.String("config", "", "Path to the config value")
	version = flag.Bool("version", false, "Print the version information")
	gitref  string
	appver  string
	builtby string
	logger  *log.Logger
)

// LoggerFunc adapts a function so it can be used as an io.Writer.
type LoggerFunc func([]byte) (int, error)

func (l LoggerFunc) Write(logbuf []byte) (n int, err error) {
	return l(logbuf)
}

// LogMessage represents a message that will be logged in JSON format.
type LogMessage struct {
	Service  string `json:"service"`
	Artifact string `json:"art-id"`
	Group    string `json:"group-id"`
	Level    string `json:"level"`
	Time     int64  `json:"timeMillis"`
	Message  string `json:"message"`
}

// NewLogMessage returns a pointer to a new instance of LogMessage.
func NewLogMessage(message string) *LogMessage {
	lm := &LogMessage{
		Service:  "jex-events",
		Artifact: "jex-events",
		Group:    "org.iplantc",
		Level:    "INFO",
		Time:     time.Now().UnixNano() / int64(time.Millisecond),
		Message:  message,
	}
	return lm
}

// LogWriter writes to stdout with a custom timestamp.
func LogWriter(logbuf []byte) (n int, err error) {
	m := NewLogMessage(string(logbuf[:]))
	j, err := json.Marshal(m)
	if err != nil {
		return 0, err
	}
	j = append(j, []byte("\n")...)
	return os.Stdout.Write(j)
}

func init() {
	logger = log.New(LoggerFunc(LogWriter), "", log.Lshortfile)
	flag.Parse()
}

// Configuration instance contain config values for jex-events.
type Configuration struct {
	AMQPURI, DBURI, EventURL, JEXURL                                      string
	ConsumerTag, HTTPListenPort                                           string
	ExchangeName, ExchangeType, RoutingKey, QueueName, QueueBindingKey    string
	ExchangeDurable, ExchangeAutodelete, ExchangeInternal, ExchangeNoWait bool
	QueueDurable, QueueAutodelete, QueueExclusive, QueueNoWait            bool
}

// ReadConfig reads JSON from 'path' and returns a pointer to a Configuration
// instance. Hopefully.
func ReadConfig(path string) (*Configuration, error) {
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
	var config Configuration
	err = json.Unmarshal(fileData, &config)
	if err != nil {
		return &config, err
	}
	return &config, nil
}

// Valid returns true if the configuration settings contain valid values.
func (c *Configuration) Valid() bool {
	retval := true
	if c.AMQPURI == "" {
		logger.Println("AMQPURI must be set in the configuration file.")
		retval = false
	}
	if c.ConsumerTag == "" {
		logger.Println("ConsumerTag must be set in the configuration file.")
		retval = false
	}
	if c.ExchangeName == "" {
		logger.Println("ExchangeName must be set in the configuration file.")
		retval = false
	}
	if c.ExchangeType == "" {
		logger.Println("ExchangeType must be set in the configuration file.")
		retval = false
	}
	if c.RoutingKey == "" {
		logger.Println("RoutingKey must be set in the configuration file.")
		retval = false
	}
	if c.QueueName == "" {
		logger.Println("QueueName must be set in the configuration file.")
		retval = false
	}
	if c.QueueBindingKey == "" {
		logger.Println("QueueBindingKey must be set in the configuration file.")
		retval = false
	}
	return retval
}

// AMQPConsumer contains the state for a connection to an AMQP broker. An
// instance of it should be capable of reading messages from an exchange.
type AMQPConsumer struct {
	URI                string
	ExchangeName       string
	ExchangeType       string
	RoutingKey         string
	ExchangeDurable    bool
	ExchangeAutodelete bool
	ExchangeInternal   bool
	ExchangeNoWait     bool
	QueueName          string
	QueueDurable       bool
	QueueAutodelete    bool
	QueueExclusive     bool
	QueueNoWait        bool
	QueueBindingKey    string
	ConsumerTag        string
	connection         *amqp.Connection
	channel            *amqp.Channel
}

// NewAMQPConsumer creates a new instance of AMQPConsumer and returns a
// pointer to it. The connection is not established at this point.
func NewAMQPConsumer(cfg *Configuration) *AMQPConsumer {
	return &AMQPConsumer{
		URI:                cfg.AMQPURI,
		ExchangeName:       cfg.ExchangeName,
		ExchangeType:       cfg.ExchangeType,
		RoutingKey:         cfg.RoutingKey,
		ExchangeDurable:    cfg.ExchangeDurable,
		ExchangeAutodelete: cfg.ExchangeAutodelete,
		ExchangeInternal:   cfg.ExchangeInternal,
		ExchangeNoWait:     cfg.ExchangeNoWait,
		QueueName:          cfg.QueueName,
		QueueBindingKey:    cfg.QueueBindingKey,
		QueueDurable:       cfg.QueueDurable,
		QueueAutodelete:    cfg.QueueAutodelete,
		QueueExclusive:     cfg.QueueExclusive,
		QueueNoWait:        cfg.QueueNoWait,
		ConsumerTag:        cfg.ConsumerTag,
	}
}

// ConnectionErrorChannel is used to send error channels to goroutines.
type ConnectionErrorChannel struct {
	Channel chan *amqp.Error
}

// MsgHandler functions will accept msgs from a Delivery channel and report
// error on the error channel.
type MsgHandler func(<-chan amqp.Delivery, <-chan int, *Databaser, string, string)

// Connect sets up a connection to an AMQP exchange
func (c *AMQPConsumer) Connect(errorChannel chan ConnectionErrorChannel) (<-chan amqp.Delivery, error) {
	var err error
	logger.Printf("Connecting to %s", c.URI)
	c.connection, err = amqp.Dial(c.URI)
	if err != nil {
		return nil, err
	}
	logger.Printf("Connected to the broker. Setting up channel...")
	c.channel, err = c.connection.Channel()
	if err != nil {
		logger.Printf("Error getting channel")
		return nil, err
	}
	logger.Printf("Initialized channel. Setting up the %s exchange...", c.ExchangeName)
	if err = c.channel.ExchangeDeclare(
		c.ExchangeName,
		c.ExchangeType,
		c.ExchangeDurable,
		c.ExchangeAutodelete,
		c.ExchangeInternal,
		c.ExchangeNoWait,
		nil,
	); err != nil {
		logger.Printf("Error setting up exchange")
		return nil, err
	}
	logger.Printf("Done setting up exchange. Setting up the %s queue...", c.QueueName)
	queue, err := c.channel.QueueDeclare(
		c.QueueName,
		c.QueueDurable,
		c.QueueAutodelete,
		c.QueueExclusive,
		c.QueueNoWait,
		nil,
	)
	if err != nil {
		logger.Printf("Error setting up queue")
		return nil, err
	}
	logger.Printf("Done setting up queue %s. Binding queue to the %s exchange.", c.QueueName, c.ExchangeName)
	if err = c.channel.QueueBind(
		queue.Name,
		c.QueueBindingKey,
		c.ExchangeName,
		c.QueueNoWait,
		nil,
	); err != nil {
		logger.Printf("Error binding the %s queue to the %s exchange", c.QueueName, c.ExchangeName)
		return nil, err
	}
	logger.Printf("Done binding the %s queue to the %s exchange", c.QueueName, c.ExchangeName)
	deliveries, err := c.channel.Consume(
		queue.Name,
		c.ConsumerTag,
		false, //autoAck
		false, //exclusive
		false, //noLocal
		false, //noWait
		nil,   //arguments
	)
	if err != nil {
		logger.Printf("Error binding the %s queue to the %s exchange", c.QueueName, c.ExchangeName)
		return nil, err
	}
	errors := c.connection.NotifyClose(make(chan *amqp.Error))
	msg := ConnectionErrorChannel{
		Channel: errors,
	}
	errorChannel <- msg // 'prime' anything receiving error messages
	return deliveries, err
}

// SetupReconnection fires up a goroutine that listens for Close() errors and
// reconnects to the AMQP server if they're encountered.
func (c *AMQPConsumer) SetupReconnection(
	errorChan chan ConnectionErrorChannel,
) {
	//errors := p.connection.NotifyClose(make(chan *amqp.Error))
	go func() {
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
	}()
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
func EventHandler(deliveries <-chan amqp.Delivery, quit <-chan int, d *Databaser, postURL string, JEXURL string) {
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
		case <-quit:
			break
		}
	}
}

// AppVersion prints version information to stdout
func AppVersion() {
	if appver != "" {
		fmt.Printf("App-Version: %s\n", appver)
	}
	if gitref != "" {
		fmt.Printf("Git-Ref: %s\n", gitref)
	}

	if builtby != "" {
		fmt.Printf("Built-By: %s\n", builtby)
	}
}

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}
	logger.Println("Starting jex-events")
	if *cfgPath == "" {
		fmt.Println("--config must be set.")
		os.Exit(-1)
	}
	config, err := ReadConfig(*cfgPath)
	if err != nil {
		logger.Print(err)
		os.Exit(-1)
	}
	logger.Println("Done reading config.")
	if !config.Valid() {
		logger.Println("Something is wrong with the jex-events config file.")
		os.Exit(-1)
	}
	logger.Println("Configuring database connection...")
	databaser, err := NewDatabaser(config.DBURI)
	if err != nil {
		logger.Print(err)
		os.Exit(-1)
	}
	logger.Println("Done configuring database connection.")

	randomizer := rand.New(rand.NewSource(time.Now().UnixNano()))
	connErrChan := make(chan ConnectionErrorChannel)
	quitHandler := make(chan int)
	consumer := NewAMQPConsumer(config)
	consumer.SetupReconnection(connErrChan)

	logger.Print("Setting up HTTP")
	SetupHTTP(config, databaser)
	logger.Print("Done setting up HTTP")

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

	EventHandler(deliveries, quitHandler, databaser, config.EventURL, config.JEXURL)
}
