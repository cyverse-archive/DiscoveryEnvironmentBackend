package configurate

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"os"
)

// Configuration instance contain config values for jex-events.
type Configuration struct {
	AMQPURI            string
	BatchGroup         string
	CondorConfig       string
	CondorLogPath      string
	ConsumerTag        string
	DBURI              string
	EventLog           string
	EventURL           string
	ExchangeName       string
	ExchangeType       string
	ExchangeDurable    bool
	ExchangeAutodelete bool
	ExchangeInternal   bool
	ExchangeNoWait     bool
	FilterFiles        string
	HTTPListenPort     string
	ICommandsPath      string
	IRODSBase          string
	IRODSUser          string
	IRODSPass          string
	IRODSHost          string
	IRODSPort          string
	IRODSZone          string
	IRODSResc          string
	JarPath            string
	JEXEvents          string
	JEXListenPort      string
	JEXURL             string
	NFSBase            string
	Path               string
	PorklockTag        string
	QueueName          string
	QueueBindingKey    string
	QueueDurable       bool
	QueueAutodelete    bool
	QueueExclusive     bool
	QueueNoWait        bool
	RequestDisk        string
	RoutingKey         string
	RunOnNFS           bool
	logger             *log.Logger
}

// New reads JSON from 'path' and returns a pointer to a Configuration
// instance. Hopefully.
func New(path string, logger *log.Logger) (*Configuration, error) {
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
	config.logger = logger
	if err != nil {
		return &config, err
	}
	return &config, nil
}

// Valid returns true if the configuration settings contain valid values.
func (c *Configuration) Valid() bool {
	retval := true
	if c.AMQPURI == "" {
		c.logger.Println("AMQPURI must be set in the configuration file.")
		retval = false
	}
	if c.ConsumerTag == "" {
		c.logger.Println("ConsumerTag must be set in the configuration file.")
		retval = false
	}
	if c.ExchangeName == "" {
		c.logger.Println("ExchangeName must be set in the configuration file.")
		retval = false
	}
	if c.ExchangeType == "" {
		c.logger.Println("ExchangeType must be set in the configuration file.")
		retval = false
	}
	if c.RoutingKey == "" {
		c.logger.Println("RoutingKey must be set in the configuration file.")
		retval = false
	}
	if c.QueueName == "" {
		c.logger.Println("QueueName must be set in the configuration file.")
		retval = false
	}
	if c.QueueBindingKey == "" {
		c.logger.Println("QueueBindingKey must be set in the configuration file.")
		retval = false
	}
	return retval
}
