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
	EventLog                                                              string
	AMQPURI, DBURI, EventURL, JEXURL                                      string
	ConsumerTag, HTTPListenPort                                           string
	ExchangeName, ExchangeType, RoutingKey, QueueName, QueueBindingKey    string
	ExchangeDurable, ExchangeAutodelete, ExchangeInternal, ExchangeNoWait bool
	QueueDurable, QueueAutodelete, QueueExclusive, QueueNoWait            bool
	logger                                                                *log.Logger
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
