package messaging

import (
	"configurate"
	"log"

	"github.com/streadway/amqp"
)

var logger *log.Logger

//Init initializes the messaging package.
func Init(l *log.Logger) {
	logger = l
}

type jexamqp struct {
	URI                string
	ExchangeName       string
	ExchangeType       string
	RoutingKey         string
	ExchangeDurable    bool
	ExchangeAutodelete bool
	ExchangeInternal   bool
	ExchangeNoWait     bool
	connection         *amqp.Connection
	channel            *amqp.Channel
}

// AMQPPublisher contains the state information for a connection to an AMQP
// broker that is capable of publishing data to an exchange.
type AMQPPublisher struct {
	jexamqp
}

// AMQPConsumer contains the state for a connection to an AMQP broker. An
// instance of it should be capable of reading messages from an exchange.
type AMQPConsumer struct {
	jexamqp
	QueueName       string
	QueueDurable    bool
	QueueAutodelete bool
	QueueExclusive  bool
	QueueNoWait     bool
	QueueBindingKey string
	ConsumerTag     string
}

// NewAMQPPublisher creates a new instance of AMQPPublisher and returns a
// pointer to it. The connection is not established at this point.
func NewAMQPPublisher(cfg *configurate.Configuration) *AMQPPublisher {
	logger.Println(cfg.AMQPURI)
	return &AMQPPublisher{
		jexamqp: jexamqp{
			URI:                cfg.AMQPURI,
			ExchangeName:       cfg.ExchangeName,
			ExchangeType:       cfg.ExchangeType,
			ExchangeDurable:    cfg.ExchangeDurable,
			ExchangeAutodelete: cfg.ExchangeAutodelete,
			ExchangeInternal:   cfg.ExchangeInternal,
			ExchangeNoWait:     cfg.ExchangeNoWait,
			RoutingKey:         cfg.RoutingKey,
		},
	}
}

// PublishString sends the body off to the configured AMQP exchange.
func (p *AMQPPublisher) PublishString(body string) error {
	return p.PublishBytes([]byte(body))
}

// PublishBytes sends off the bytes to the AMQP broker.
func (p *AMQPPublisher) PublishBytes(body []byte) error {
	logger.Printf("Publishing message to the %s exchange using routing key %s", p.ExchangeName, p.RoutingKey)
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
	logger.Printf("Done publishing message.")
	return nil
}

// connect performs connection initialization logic that is common to both
// AMQPPublishers and AMQPConsumers.
func (j *jexamqp) connect() error {
	var err error
	logger.Printf("Connecting to %s", j.URI)
	j.connection, err = amqp.Dial(j.URI)
	if err != nil {
		return err
	}
	logger.Printf("Connected to the broker. Setting up channel...")
	j.channel, err = j.connection.Channel()
	if err != nil {
		logger.Printf("Error getting channel")
		return err
	}
	logger.Printf("Initialized channel. Setting up the %s exchange...", j.ExchangeName)
	if err = j.channel.ExchangeDeclare(
		j.ExchangeName,
		j.ExchangeType,
		j.ExchangeDurable,
		j.ExchangeAutodelete,
		j.ExchangeInternal,
		j.ExchangeNoWait,
		nil,
	); err != nil {
		logger.Printf("Error setting up exchange")
		return err
	}
	logger.Println("Done setting up exchange.")
	return nil
}

// finishconnection performs the final bits of initialization logic that is
// common to both AMQPPublishers and AMQPConsumers. Should be called last in the
// Connect() functions.
func (j *jexamqp) finishconnection(errorChannel chan ConnectionError) {
	// The channel created here is wrapped in a ConnectionError
	errors := j.connection.NotifyClose(make(chan *amqp.Error))
	msg := ConnectionError{
		Channel: errors,
	}
	errorChannel <- msg // triggers the reconnection logic
}

// Connect will attempt to connect to the AMQP broker, create/use the configured
// exchange, and create a new channel. Make sure you call the Close method when
// you are done, most likely with a defer statement.
func (p *AMQPPublisher) Connect(errorChannel chan ConnectionError) error {
	err := p.connect()
	if err != nil {
		return err
	}
	p.finishconnection(errorChannel)
	return err
}

// Connect sets up a connection to an AMQP exchange
func (c *AMQPConsumer) Connect(errorChannel chan ConnectionError) (<-chan amqp.Delivery, error) {
	err := c.connect()
	logger.Printf("Setting up the %s queue...\n", c.QueueName)
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
	c.finishconnection(errorChannel)
	return deliveries, err
}

// Close calls Close() on the underlying AMQP connection.
func (p *AMQPPublisher) Close() {
	p.connection.Close()
}

// NewAMQPConsumer creates a new instance of AMQPConsumer and returns a
// pointer to it. The connection is not established at this point.
func NewAMQPConsumer(cfg *configurate.Configuration) *AMQPConsumer {
	return &AMQPConsumer{
		jexamqp: jexamqp{
			ExchangeName:       cfg.ExchangeName,
			ExchangeType:       cfg.ExchangeType,
			RoutingKey:         cfg.RoutingKey,
			ExchangeDurable:    cfg.ExchangeDurable,
			ExchangeAutodelete: cfg.ExchangeAutodelete,
			ExchangeInternal:   cfg.ExchangeInternal,
			ExchangeNoWait:     cfg.ExchangeNoWait,
			URI:                cfg.AMQPURI,
		},
		QueueName:       cfg.QueueName,
		QueueBindingKey: cfg.QueueBindingKey,
		QueueDurable:    cfg.QueueDurable,
		QueueAutodelete: cfg.QueueAutodelete,
		QueueExclusive:  cfg.QueueExclusive,
		QueueNoWait:     cfg.QueueNoWait,
		ConsumerTag:     cfg.ConsumerTag,
	}
}

// ConnectionError is used to send error channels to goroutines.
type ConnectionError struct {
	Channel chan *amqp.Error
}

type reconnector func(chan ConnectionError)

// SetupReconnection fires up a goroutine that listens for Close() errors and
// reconnects to the AMQP server if they're encountered.
func SetupReconnection(errorChan chan ConnectionError, r reconnector) {
	go r(errorChan)
}
