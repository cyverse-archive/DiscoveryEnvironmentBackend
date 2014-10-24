package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"regexp"

	"github.com/streadway/amqp"
)

var (
	cfgPath = flag.String("config", "", "Path to the config value")
)

func init() {
	flag.Parse()
}

// Configuration instance contain config values for jex-events.
type Configuration struct {
	AMQPURI                                                               string
	ConsumerTag                                                           string
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
		log.Println("AMQPURI must be set in the configuration file.")
		retval = false
	}
	if c.ConsumerTag == "" {
		log.Println("ConsumerTag must be set in the configuration file.")
		retval = false
	}
	if c.ExchangeName == "" {
		log.Println("ExchangeName must be set in the configuration file.")
		retval = false
	}
	if c.ExchangeType == "" {
		log.Println("ExchangeType must be set in the configuration file.")
		retval = false
	}
	if c.RoutingKey == "" {
		log.Println("RoutingKey must be set in the configuration file.")
		retval = false
	}
	if c.QueueName == "" {
		log.Println("QueueName must be set in the configuration file.")
		retval = false
	}
	if c.QueueBindingKey == "" {
		log.Println("QueueBindingKey must be set in the configuration file.")
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
	channel chan *amqp.Error
}

// MsgHandler functions will accept msgs from a Delivery channel and report
// error on the error channel.
type MsgHandler func(<-chan amqp.Delivery, <-chan int)

// Connect sets up a connection to an AMQP exchange
func (c *AMQPConsumer) Connect(errorChannel chan ConnectionErrorChannel) (<-chan amqp.Delivery, error) {
	var err error
	log.Printf("Connecting to %s", c.URI)
	c.connection, err = amqp.Dial(c.URI)
	if err != nil {
		return nil, err
	}
	log.Printf("Connected to the broker. Setting up channel...")
	c.channel, err = c.connection.Channel()
	if err != nil {
		log.Printf("Error getting channel")
		return nil, err
	}
	log.Printf("Initialized channel. Setting up the %s exchange...", c.ExchangeName)
	if err = c.channel.ExchangeDeclare(
		c.ExchangeName,
		c.ExchangeType,
		c.ExchangeDurable,
		c.ExchangeAutodelete,
		c.ExchangeInternal,
		c.ExchangeNoWait,
		nil,
	); err != nil {
		log.Printf("Error setting up exchange")
		return nil, err
	}
	log.Printf("Done setting up exchange. Setting up the %s queue...", c.QueueName)
	queue, err := c.channel.QueueDeclare(
		c.QueueName,
		c.QueueDurable,
		c.QueueAutodelete,
		c.QueueExclusive,
		c.QueueNoWait,
		nil,
	)
	if err != nil {
		log.Printf("Error setting up queue")
		return nil, err
	}
	log.Printf("Done setting up queue %s. Binding queue to the %s exchange.", c.QueueName, c.ExchangeName)
	if err = c.channel.QueueBind(
		queue.Name,
		c.QueueBindingKey,
		c.ExchangeName,
		c.QueueNoWait,
		nil,
	); err != nil {
		log.Printf("Error binding the %s queue to the %s exchange", c.QueueName, c.ExchangeName)
		return nil, err
	}
	log.Printf("Done binding the %s queue to the %s exchange", c.QueueName, c.ExchangeName)
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
		log.Printf("Error binding the %s queue to the %s exchange", c.QueueName, c.ExchangeName)
		return nil, err
	}
	errors := c.connection.NotifyClose(make(chan *amqp.Error))
	msg := ConnectionErrorChannel{
		channel: errors,
	}
	errorChannel <- msg // 'prime' anything receiving error messages
	return deliveries, err
}

// SetupReconnection fires up a goroutine that listens for Close() errors and
// reconnects to the AMQP server if they're encountered.
func (c *AMQPConsumer) SetupReconnection(errorChan chan ConnectionErrorChannel, handler MsgHandler, quitHandler chan int) {
	//errors := p.connection.NotifyClose(make(chan *amqp.Error))
	go func() {
		var exitChan chan *amqp.Error
		reconfig := true
		for {
			if reconfig {
				msg := <-errorChan     // the Connect() function sends out a msg to 'prime' this goroutine
				exitChan = msg.channel // the exitChan to listen on comes from the initial priming msg
			}
			select {
			case exitError, ok := <-exitChan:
				if !ok { // this occurs when an error causes the connection to close.
					log.Println("Exit channel closed.")
					quitHandler <- 1
					deliveries, err := c.Connect(errorChan)
					if err != nil {
						log.Print("Error reconnecting to server, exiting.")
						log.Print(err)
					}
					handler(deliveries, quitHandler)
					reconfig = true //
				} else {
					log.Println(exitError)
					quitHandler <- 1
					deliveries, err := c.Connect(errorChan)
					if err != nil {
						log.Print("Error reconnecting to server, exiting.")
						log.Print(err)
					}
					handler(deliveries, quitHandler)
					reconfig = false
				}
			}
		}
	}()
}

// Event contains an event received from the AMQP broker and parsed from JSON.
type Event struct {
	Event       string
	Hash        string
	EventNumber string
	ID          string
	Date        string
	Time        string
	Msg         string
}

func (e *Event) String() string {
	retval := fmt.Sprintf("EventNumber: %s\tID: %s\tDate: %s\tTime: %s\tMsg: %s",
		e.EventNumber,
		e.ID,
		e.Date,
		e.Time,
		e.Msg,
	)
	return retval
}

// EventHandler processes incoming event messages
func EventHandler(deliveries <-chan amqp.Delivery, quit <-chan int) {
	for {
		select {
		case delivery := <-deliveries:
			body := delivery.Body
			delivery.Ack(false) //We're not doing batch deliveries, which is what the false means
			var event Event
			err := json.Unmarshal(body, &event)
			if err != nil {
				log.Print(err)
				log.Print(string(body[:]))
			}
			EventParser(&event)
			log.Println(event.String())
		case <-quit:
			break
		}
	}
}

// EventParser extracts info from an event string.
func EventParser(event *Event) {
	r := regexp.MustCompile("^([0-9]{3}) (\\([0-9]+(?:\\.[0-9]+){2}\\)) ([0-9/]+) ([0-9:]+) (.*)\\n")
	matches := r.FindStringSubmatch(event.Event)
	matchesLength := len(matches)
	if matchesLength >= 2 {
		event.EventNumber = matches[1]
	}
	if matchesLength >= 3 {
		event.ID = matches[2]
	}
	if matchesLength >= 4 {
		event.Date = matches[3]
	}
	if matchesLength >= 5 {
		event.Time = matches[4]
	}
	if matchesLength >= 6 {
		event.Msg = matches[5]
	}
}

func main() {
	if *cfgPath == "" {
		fmt.Println("--config must be set.")
		os.Exit(-1)
	}
	config, err := ReadConfig(*cfgPath)
	if err != nil {
		log.Print(err)
		os.Exit(-1)
	}
	if !config.Valid() {
		os.Exit(-1)
	}
	connErrChan := make(chan ConnectionErrorChannel)
	quitHandler := make(chan int)
	consumer := NewAMQPConsumer(config)
	consumer.SetupReconnection(connErrChan, EventHandler, quitHandler)
	deliveries, err := consumer.Connect(connErrChan)
	if err != nil {
		log.Print(err)
		os.Exit(-1)
	}
	EventHandler(deliveries, quitHandler)
}
