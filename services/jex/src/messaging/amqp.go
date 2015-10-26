package messaging

import (
	"encoding/json"
	"logcabin"
	"math/rand"
	"model"
	"time"

	"github.com/streadway/amqp"
)

//Command is tells the receiver of a JobRequest which action to perform
type Command int

// JobState defines a valid state for a job.
type JobState string

// StatusCode defines a valid exit code for a job.
type StatusCode int

var (
	logger = logcabin.New()

	//LaunchCommand is the string used in LaunchCo
	LaunchCommand = "LAUNCH"

	//JobsExchange is the name of the exchange that job related info is passed around.
	JobsExchange = "jobs"

	//LaunchesKey is the routing/binding key for job launch request messages.
	LaunchesKey = "jobs.launches"

	//UpdatesKey is the routing/binding key for job update messages.
	UpdatesKey = "jobs.updates"

	//StopsKey is the routing/binding key for job stop request messages.
	StopsKey = "jobs.stops"

	//CommandsKey is the routing/binding key for job command messages.
	CommandsKey = "jobs.commands"

	//QueuedState is when a job is queued.
	QueuedState JobState = "Queued"

	//SubmittedState is when a job has been submitted.
	SubmittedState JobState = "Submitted"

	//RunningState is when a job is running.
	RunningState JobState = "Running"

	//SucceededState is when a job has successfully completed the required steps.
	SucceededState JobState = "Complete"

	//FailedState is when a job has failed. Duh.
	FailedState JobState = "Failed"
)

const (
	//Launch tells the receiver of a JobRequest to launch the job
	Launch Command = iota

	//Stop tells the receiver of a JobRequest to stop a job
	Stop
)

const (
	// Success is the exit code used when the required commands execute correctly.
	Success StatusCode = iota

	// StatusDockerPullFailed is the exit code when a 'docker pull' fails.
	StatusDockerPullFailed

	// StatusDockerCreateFailed is the exit code when a 'docker create' fails.
	StatusDockerCreateFailed

	// StatusInputFailed is the exit code when an input download fails.
	StatusInputFailed

	// StatusStepFailed is the exit code when a step in the job fails.
	StatusStepFailed

	// StatusOutputFailed is the exit code when the output upload fails.
	StatusOutputFailed

	// StatusKilled is the exit code when the job is killed.
	StatusKilled

	// StatusTimeLimit is the exit code when the job is killed due to the time
	// limit being reached.
	StatusTimeLimit

	// StatusBadDuration is the exit code when the job is killed because an
	// unparseable job duration was sent to it.
	StatusBadDuration
)

// JobRequest is a generic request type for job related requests.
type JobRequest struct {
	Job     *model.Job
	Command Command
	Message string
	Version int
}

// StopRequest contains the information needed to stop a job
type StopRequest struct {
	Reason       string
	Username     string
	Version      int
	InvocationID string
}

// UpdateMessage contains the information needed to broadcast a change in state
// for a job.
type UpdateMessage struct {
	Job     *model.Job
	Version int
	State   JobState
	Message string
}

// NewStopRequest returns a *JobRequest that has been constructed to be a
// stop request for a running job.
func NewStopRequest() *StopRequest {
	return &StopRequest{
		Version: 0,
	}
}

// NewLaunchRequest returns a *JobRequest that has been constructed to be a
// launch request for the provided job.
func NewLaunchRequest(j *model.Job) *JobRequest {
	return &JobRequest{
		Job:     j,
		Command: Launch,
		Version: 0,
	}
}

// MessageHandler defines a type for amqp.Delivery handlers.
type MessageHandler func(amqp.Delivery)

type aggregationMessage struct {
	handler  MessageHandler
	delivery *amqp.Delivery
}

type consumer struct {
	exchange string
	queue    string
	key      string
	handler  MessageHandler
}

type publisher struct {
	exchange string
	channel  *amqp.Channel
}

// Client encapsulates the information needed to interact via AMQP.
type Client struct {
	uri             string
	connection      *amqp.Connection
	aggregationChan chan aggregationMessage
	errors          chan *amqp.Error
	consumers       []*consumer
	publisher       *publisher
}

// NewClient returns a new *Client. It will block until the connection succeeds.
func NewClient(uri string) *Client {
	c := &Client{}
	randomizer := rand.New(rand.NewSource(time.Now().UnixNano()))
	c.uri = uri
	logger.Println("Attempting AMQP connection...")
	var connection *amqp.Connection
	var err error
	for {
		connection, err = amqp.Dial(c.uri)
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
	c.connection = connection
	c.aggregationChan = make(chan aggregationMessage)
	c.errors = c.connection.NotifyClose(make(chan *amqp.Error))
	return c
}

// Listen will wait for messages and pass them off to handlers, which run in
// their own goroutine.
func (c *Client) Listen() {
	var consumers []*consumer
	init := func() {
		for _, cs := range c.consumers {
			c.initconsumer(cs)
		}
	}
	init()
	for _, cs := range c.consumers {
		consumers = append(consumers, cs)
	}
	for {
		select {
		case err := <-c.errors:
			logger.Printf("An error in the connection to the AMQP broker occurred:\n%s", err)
			c = NewClient(c.uri)
			c.consumers = consumers
			init()
		case msg := <-c.aggregationChan:
			go func() {
				msg.handler(*msg.delivery)
			}()
		}
	}
}

// Close closes the connection to the AMQP broker.
func (c *Client) Close() {
	c.connection.Close()
}

// AddConsumer adds a consumer to the list of consumers that need to be created
// each time the client is set up. Note that this just adds the consumers to a
// list, it doesn't actually start handling messages yet. You need to call
// Listen() for that.
func (c *Client) AddConsumer(exchange, queue, key string, handler MessageHandler) {
	cs := &consumer{
		exchange: exchange,
		queue:    queue,
		key:      key,
		handler:  handler,
	}
	c.consumers = append(c.consumers, cs)
}

func (c *Client) initconsumer(cs *consumer) error {
	channel, err := c.connection.Channel()
	if err != nil {
		return err
	}
	err = channel.ExchangeDeclare(
		cs.exchange, //name
		"topic",     //kind
		true,        //durable
		false,       //auto-delete
		false,       //internal
		false,       //no-wait
		nil,         //args
	)
	_, err = channel.QueueDeclare(
		cs.queue,
		true,  //durable
		false, //auto-delete
		false, //internal
		false, //no-wait
		nil,   //args
	)
	err = channel.QueueBind(
		cs.queue,
		cs.key,
		cs.exchange,
		false, //no-wait
		nil,   //args
	)

	d, err := channel.Consume(
		cs.queue,
		"",    //consumer tag - auto-assigned in this case
		false, //auto-ack
		false, //exclusive
		false, //no-local
		false, //no-wait
		nil,   //args
	)
	if err != nil {
		return err
	}
	go func() {
		for msg := range d {
			c.aggregationChan <- aggregationMessage{
				handler:  cs.handler,
				delivery: &msg,
			}
		}
	}()
	return err
}

// SetupPublishing initializes the publishing functionality of the client.
// Call this before calling Publish.
func (c *Client) SetupPublishing(exchange string) error {
	channel, err := c.connection.Channel()
	if err != nil {
		return err
	}
	err = channel.ExchangeDeclare(
		exchange, //name
		"topic",  //kind
		true,     //durable
		false,    //auto-delete
		false,    //internal
		false,    //no-wait
		nil,      //args
	)
	if err != nil {
		return err
	}
	p := &publisher{
		exchange: exchange,
		channel:  channel,
	}
	c.publisher = p
	return err
}

// Publish sends a message to the configured exchange with a routing key set to
// the value of 'key'.
func (c *Client) Publish(key string, body []byte) error {
	msg := amqp.Publishing{
		DeliveryMode: amqp.Persistent,
		Timestamp:    time.Now(),
		ContentType:  "text/plain",
		Body:         body,
	}
	err := c.publisher.channel.Publish(
		c.publisher.exchange,
		key,
		false, //mandatory
		false, //immediate
		msg,
	)
	return err
}

// PublishJobUpdate sends a mess to the configured exchange with a routing key of
// "jobs.updates"
func (c *Client) PublishJobUpdate(u *UpdateMessage) error {
	msgJSON, err := json.Marshal(u)
	if err != nil {
		return err
	}
	return c.Publish(UpdatesKey, msgJSON)
}
