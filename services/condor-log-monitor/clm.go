// condor-log-monitor
//
// Tails the configured EVENT_LOG on a condor submission node, parses events from it,
// and pushes events out to an AMQP broker.
//
// condor-log-monitor will make an attempt at detecting rolling over log files and
// recovering from extended downtime, but whether or not a full recovery is possible
// depends on how the Condor logging is configured. It is still possible to lose
// messages if condor-log-monitor is down for a while and Condor rotates files
// out too many times.
//
// Condor attempts to recover from downtime by recording a tombstone file that
// records the inode number, last modified date, processing date, and last
// processed position. At start up clm will look for the tombstoned file and will
// attempt to start processing from that point forward.
//
// If the inode of the new file doesn't match the inode contained in the tombstone,
// then scan the directory for all of the old log files and collect their inodes
// and last modified dates. Sort the old log files from oldest to newest -- based
// on the last modified date -- and iterate through them. Find the file that matches
// the inode of the file from the tombstone and process it starting from the position
// recorded in the tombstone. Then, process all of remaining files until you reach
// the current log file. Process the current log file and record a new tombstone.
// Do not delete the old tombstone until you're ready to record a new one.
//
// If condor-log-monitor has been down for so long that the tombstoned log file no
// longer exists, process all of the log file in order from oldest to newest. Record
// a new tombstone when you reach the end of the newest log file.

package main

import (
	"crypto/sha256"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"regexp"
	"syscall"
	"time"

	"github.com/ActiveState/tail"
	"github.com/streadway/amqp"
)

var (
	cfgPath = flag.String("config", "", "Path to the config file.")
	logPath = flag.String("event-log", "", "Path to the log file.")
)

func init() {
	flag.Parse()
}

// Configuration contains the setting read from a config file.
type Configuration struct {
	EventLog                               string
	AMQPURI                                string
	ExchangeName, ExchangeType, RoutingKey string
	Durable, Autodelete, Internal, NoWait  bool
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

// AMQPPublisher contains the state information for a connection to an AMQP
// broker that is capable of publishing data to an exchange.
type AMQPPublisher struct {
	URI          string
	ExchangeName string
	ExchangeType string
	RoutingKey   string
	Durable      bool
	Autodelete   bool
	Internal     bool
	NoWait       bool
	connection   *amqp.Connection
	channel      *amqp.Channel
}

// NewAMQPPublisher creates a new instance of AMQPPublisher and returns a
// pointer to it. The connection is not established at this point.
func NewAMQPPublisher(cfg *Configuration) *AMQPPublisher {
	return &AMQPPublisher{
		URI:          cfg.AMQPURI,
		ExchangeName: cfg.ExchangeName,
		ExchangeType: cfg.ExchangeType,
		RoutingKey:   cfg.RoutingKey,
		Durable:      cfg.Durable,
		Autodelete:   cfg.Autodelete,
		Internal:     cfg.Internal,
		NoWait:       cfg.NoWait,
	}
}

// ConnectionErrorChan is used to send error channels to goroutines.
type ConnectionErrorChan struct {
	channel chan *amqp.Error
}

// Connect will attempt to connect to the AMQP broker, create/use the configured
// exchange, and create a new channel. Make sure you call the Close method when
// you are done, most likely with a defer statement.
func (p *AMQPPublisher) Connect(errorChan chan ConnectionErrorChan) error {
	connection, err := amqp.Dial(p.URI)
	if err != nil {
		return err
	}
	p.connection = connection

	channel, err := p.connection.Channel()
	if err != nil {
		return err
	}

	err = channel.ExchangeDeclare(
		p.ExchangeName,
		p.ExchangeType,
		p.Durable,
		p.Autodelete,
		p.Internal,
		p.NoWait,
		nil, //arguments
	)
	if err != nil {
		return err
	}
	p.channel = channel
	errors := p.connection.NotifyClose(make(chan *amqp.Error))
	msg := ConnectionErrorChan{
		channel: errors,
	}
	errorChan <- msg
	return nil
}

// SetupReconnection fires up a goroutine that listens for Close() errors and
// reconnects to the AMQP server if they're encountered.
func (p *AMQPPublisher) SetupReconnection(errorChan chan ConnectionErrorChan) {
	//errors := p.connection.NotifyClose(make(chan *amqp.Error))
	go func() {
		var exitChan chan *amqp.Error
		reconfig := true
		for {
			if reconfig {
				msg := <-errorChan
				exitChan = msg.channel
			}
			select {
			case exitError, ok := <-exitChan:
				if !ok {
					log.Println("Exit channel closed.")
					reconfig = true
				} else {
					log.Println(exitError)
					p.Connect(errorChan)
					reconfig = false
				}
			}
		}
	}()
}

// PublishString sends the body off to the configured AMQP exchange.
func (p *AMQPPublisher) PublishString(body string) error {
	return p.PublishBytes([]byte(body))
}

// PublishBytes sends off the bytes to the AMQP broker.
func (p *AMQPPublisher) PublishBytes(body []byte) error {
	if err := p.channel.Publish(
		p.ExchangeName,
		p.RoutingKey,
		false, //mandatory?
		false, //immediate?
		amqp.Publishing{
			Headers:         amqp.Table{},
			ContentType:     "text/plain",
			ContentEncoding: "",
			Body:            body,
			DeliveryMode:    amqp.Transient,
			Priority:        0,
		},
	); err != nil {
		return err
	}
	return nil
}

// Close calls Close() on the underlying AMQP connection.
func (p *AMQPPublisher) Close() {
	p.connection.Close()
}

// PublishableEvent is a type that contains the information that gets sent to
// the AMQP broker. It's meant to be marshalled into JSON or some other format.
type PublishableEvent struct {
	Event string
	Hash  string
}

// NewPublishableEvent creates returns a pointer to a newly created instance
// of PublishableEvent.
func NewPublishableEvent(event string) *PublishableEvent {
	hashBytes := sha256.Sum256([]byte(event))
	return &PublishableEvent{
		Event: event,
		Hash:  string(hashBytes[:]),
	}
}

// ParseEvent will tail a file and print out each event as it comes through.
// The AMQPPublisher that is passed in should already have its connection
// established. This function does not call Close() on it.
func ParseEvent(filepath string, pub *AMQPPublisher) error {
	startRegex := "^[\\d][\\d][\\d]\\s.*"
	endRegex := "^\\.\\.\\..*"
	foundStart := false
	var eventlines string //accumulates lines in an event entry

	t, err := tail.TailFile(filepath, tail.Config{
		ReOpen: true,
		Follow: true,
		Poll:   true,
	})
	for line := range t.Lines {
		text := line.Text
		if !foundStart {
			matchedStart, err := regexp.MatchString(startRegex, text)
			if err != nil {
				return err
			}
			if matchedStart {
				foundStart = true
				eventlines = eventlines + text + "\n"
				if err != nil {
					return err
				}
			}
		} else {
			matchedEnd, err := regexp.MatchString(endRegex, text)
			if err != nil {
				return err
			}
			eventlines = eventlines + text + "\n"
			if matchedEnd {
				fmt.Println(eventlines)
				pubEvent := NewPublishableEvent(eventlines)
				pubJSON, err := json.Marshal(pubEvent)
				if err != nil {
					return err
				}
				if err = pub.PublishBytes(pubJSON); err != nil {
					fmt.Println(err)
				}
				eventlines = ""
				foundStart = false
			}
		}
	}
	return err
}

//TombstoneAction denotes the kind of action a TombstoneMsg represents.
type TombstoneAction int

const (
	//Set says that the TombstoneMsg contains a set action.
	Set TombstoneAction = iota

	//Get says that the TombstoneMsg contains a get action.
	Get

	//Quit says that the TombstoneMsg contains a quit action.
	Quit
)

//TombstoneMsg represents a message sent to a goroutine that processes tombstone
//related operations. The Data field contains information that the tombstone
//goroutine may take action on, depending on the Action. Set messages will set
//the current value of the tombstone to the value in the Data field. Get messages
//will return the current value of the tombstone on the Reply channel. Quit
//messages tell the goroutine to shut down as cleanly as possible. The Reply
//channel may be used on certain operations to pass back data from the goroutine
//in response to a received TombstoneMsg.
type TombstoneMsg struct {
	Action TombstoneAction
	Data   Tombstone
	Reply  chan interface{}
}

type Tombstone struct {
	CurrentPos int64
	Date       time.Time
	LogLastMod time.Time
	Inode      uint64
}

func InodeFromPath(path string) (uint64, error) {
	openFile, err := os.Open(path)
	if err != nil {
		return 0, err
	}
	ino, err := InodeFromFile(openFile)
	if err != nil {
		return 0, err
	}
	return ino, nil
}

func InodeFromFile(openFile *os.File) (uint64, error) {
	fileInfo, err := openFile.Stat()
	if err != nil {
		return 0, err
	}
	sys := fileInfo.Sys().(*syscall.Stat_t)
	return sys.Ino, nil
}

func NewTombstoneFromPath(path string) (*Tombstone, error) {
	openFile, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	tombstone, err := NewTombstoneFromFile(openFile)
	if err != nil {
		return nil, err
	}
	return tombstone, nil
}

func NewTombstoneFromFile(openFile *os.File) (*Tombstone, error) {
	fileInfo, err := openFile.Stat()
	if err != nil {
		return nil, err
	}
	inode, err := InodeFromFile(openFile)
	if err != nil {
		return nil, err
	}
	currentPos, err := openFile.Seek(0, os.SEEK_CUR)
	if err != nil {
		return nil, err
	}
	tombstone := &Tombstone{
		CurrentPos: currentPos,
		Date:       time.Now(),
		LogLastMod: fileInfo.ModTime(),
		Inode:      inode,
	}
	return tombstone, nil
}

func main() {
	if *cfgPath == "" {
		fmt.Printf("--config must be set.")
		os.Exit(-1)
	}
	cfg, err := ReadConfig(*cfgPath)
	if err != nil {
		fmt.Println(err)
	}
	errChan := make(chan ConnectionErrorChan)
	pub := NewAMQPPublisher(cfg)
	pub.SetupReconnection(errChan)
	if err = pub.Connect(errChan); err != nil {
		fmt.Println(err)
		os.Exit(-1)
	}
	exitChan := make(chan int)
	go func() {
		err := ParseEvent(cfg.EventLog, pub)
		if err != nil {
			fmt.Println(err)
		}
		exitChan <- 1
	}()

	fmt.Println(cfg)
	<-exitChan
}
