package messaging

import (
	"configurate"
	"logcabin"
	"math/rand"
	"time"

	"github.com/olebedev/config"
	"github.com/streadway/amqp"
)

var logger = logcabin.New()

//Init initializes the messaging package.
func Init(l *logcabin.Lincoln) {
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

func new(cfg *config.Config) (*jexamqp, error) {
	uri, err := cfg.String("uri")
	if err != nil {
		return nil, err
	}
	name, err := cfg.String("exchange.name")
	if err != nil {
		return nil, err
	}
	etype, err := cfg.String("exchange.type")
	if err != nil {
		return nil, err
	}
	durable, err := cfg.Bool("exchange.durable")
	if err != nil {
		return nil, err
	}
	autod, err := cfg.Bool("exchange.autodelete")
	if err != nil {
		return nil, err
	}
	internal, err := cfg.Bool("exchange.internal")
	if err != nil {
		return nil, err
	}
	nowait, err := cfg.Bool("exchange.nowait")
	if err != nil {
		return nil, err
	}
	rkey, err := cfg.String("routingkey")
	if err != nil {
		return nil, err
	}
	return &jexamqp{
		URI:                uri,
		ExchangeName:       name,
		ExchangeType:       etype,
		ExchangeDurable:    durable,
		ExchangeAutodelete: autod,
		ExchangeInternal:   internal,
		ExchangeNoWait:     nowait,
		RoutingKey:         rkey,
	}, nil
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
func NewAMQPPublisher(cfg *config.Config) (*AMQPPublisher, error) {
	logger.Println(cfg.String("uri"))
	j, err := new(cfg)
	if err != nil {
		return nil, err
	}
	return &AMQPPublisher{jexamqp: *j}, nil
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
	go func() {
		errorChannel <- msg // triggers the reconnection logic
	}()
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
	if err != nil {
		logger.Print(err)
		return nil, err
	}
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
func NewAMQPConsumer(cfg *config.Config) (*AMQPConsumer, error) {
	j, err := new(cfg)
	if err != nil {
		return nil, err
	}
	qname, err := cfg.String("queue.name")
	if err != nil {
		return nil, err
	}
	bindingKey, err := cfg.String("queue.bindingkey")
	if err != nil {
		return nil, err
	}
	durable, err := cfg.Bool("queue.durable")
	if err != nil {
		return nil, err
	}
	autod, err := cfg.Bool("queue.autodelete")
	if err != nil {
		return nil, err
	}
	exclusive, err := cfg.Bool("queue.exclusive")
	if err != nil {
		return nil, err
	}
	noWait, err := cfg.Bool("queue.nowait")
	if err != nil {
		return nil, err
	}
	consumerTag, err := cfg.String("consumertag")
	if err != nil {
		return nil, err
	}
	return &AMQPConsumer{
		jexamqp:         *j,
		QueueName:       qname,
		QueueBindingKey: bindingKey,
		QueueDurable:    durable,
		QueueAutodelete: autod,
		QueueExclusive:  exclusive,
		QueueNoWait:     noWait,
		ConsumerTag:     consumerTag,
	}, nil
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

// MessageClient encapsulates the state needed to consume messages from an AMQP
// queue.
type MessageClient struct {
	consumer   *AMQPConsumer
	errorChan  chan ConnectionError
	deliveries <-chan amqp.Delivery
}

// MessageHandler is a function that can take in an amqp.Delivery and do something
// with it.
type MessageHandler func(amqp.Delivery)

// Run executes handler in a separate goroutine for every message received. Does
// not return.
func (m *MessageClient) Run(handler MessageHandler) {
	msg := <-m.errorChan
	exitChan := msg.Channel
	for {
		select {
		case exitError, ok := <-exitChan:
			if !ok {
				logger.Println("exit channel close; resetting it")
				m.errorChan = make(chan ConnectionError)
				logger.Println("done resetting exit channel")
			}
			logger.Printf("error encountered connecting to the message broker:\n%s", exitError)
			m.Connect()
			msg = <-m.errorChan
			exitChan = msg.Channel
		case delivery := <-m.deliveries:
			go handler(delivery)
		}
	}
}

// Connect finishes off the connection to the AMQP broker.
func (m *MessageClient) Connect() {
	var err error
	randomizer := rand.New(rand.NewSource(time.Now().UnixNano()))
	var deliveries <-chan amqp.Delivery
	for {
		logger.Println("Attempting AMQP connection...")
		deliveries, err = m.consumer.Connect(m.errorChan)
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
	m.deliveries = deliveries
}

// MessageConsumer returns a *MessageClient that is already configured and ready
// to go.
func MessageConsumer() (*MessageClient, error) {
	cfg, err := configurate.C.Get("amqp.condor")
	if err != nil {
		return nil, err
	}
	consumer, err := NewAMQPConsumer(cfg)
	if err != nil {
		return nil, err
	}
	errChan := make(chan ConnectionError)
	cl := &MessageClient{
		consumer:  consumer,
		errorChan: errChan,
	}
	cl.Connect()
	return cl, err
}
