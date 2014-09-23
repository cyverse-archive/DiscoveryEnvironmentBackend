package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"regexp"

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

// Publish sends the body off to the configured AMQP exchange.
func (p *AMQPPublisher) Publish(body string) error {
	if err := p.channel.Publish(
		p.ExchangeName,
		p.RoutingKey,
		false, //mandatory?
		false, //immediate?
		amqp.Publishing{
			Headers:         amqp.Table{},
			ContentType:     "text/plain",
			ContentEncoding: "",
			Body:            []byte(body),
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
				if err = pub.Publish(eventlines); err != nil {
					fmt.Println(err)
				}
				eventlines = ""
				foundStart = false
			}
		}
	}
	return err
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
