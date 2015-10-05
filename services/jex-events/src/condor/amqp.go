package condor

import (
	"configurate"
	"math/rand"
	"messaging"
	"time"

	"github.com/streadway/amqp"
)

// MessageClient encapsulates the state needed to consume messages from an AMQP
// queue.
type MessageClient struct {
	consumer   *messaging.AMQPConsumer
	errorChan  chan messaging.ConnectionError
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
				m.errorChan = make(chan messaging.ConnectionError)
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
	consumer, err := messaging.NewAMQPConsumer(cfg)
	if err != nil {
		return nil, err
	}
	errChan := make(chan messaging.ConnectionError)
	cl := &MessageClient{
		consumer:  consumer,
		errorChan: errChan,
	}
	cl.Connect()
	return cl, err
}
