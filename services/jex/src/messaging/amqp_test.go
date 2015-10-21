package messaging

import (
	"fmt"
	"model"
	"os"
	"reflect"
	"testing"
	"time"

	"github.com/streadway/amqp"
)

func shouldrun() bool {
	if os.Getenv("RABBIT_PORT_5672_TCP_ADDR") != "" {
		return true
	}
	return false
}

func uri() string {
	addr := os.Getenv("RABBIT_PORT_5672_TCP_ADDR")
	port := os.Getenv("RABBIT_PORT_5672_TCP_PORT")
	return fmt.Sprintf("amqp://guest:guest@%s:%s/", addr, port)
}

func TestConstants(t *testing.T) {
	expected := 0
	actual := int(Launch)
	if actual != expected {
		t.Errorf("Launch was %d instead of %d", actual, expected)
	}
	expected = 1
	actual = int(Stop)
	if actual != expected {
		t.Errorf("Stop was %d instead of %d", actual, expected)
	}
	expected = 0
	actual = int(Success)
	if actual != expected {
		t.Errorf("Success was %d instead of %d", actual, expected)
	}
}

func TestNewStopRequest(t *testing.T) {
	actual := NewStopRequest()
	expected := &StopRequest{Version: 0}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("NewStopRequest returned:\n%#v\n\tinstead of:\n%#v", actual, expected)
	}
}

func TestNewLaunchRequest(t *testing.T) {
	job := &model.Job{}
	actual := NewLaunchRequest(job)
	expected := &JobRequest{
		Version: 0,
		Job:     job,
		Command: Launch,
	}
	if !reflect.DeepEqual(actual, expected) {
		t.Errorf("NewLaunchRequest returned:\n%#v\n\tinstead of:\n%#v", actual, expected)
	}
}

func TestNewClient(t *testing.T) {
	if !shouldrun() {
		return
	}
	actual := NewClient(uri())
	defer actual.Close()
	expected := uri()
	if actual.uri != expected {
		t.Errorf("Client's uri was %s instead of %s", actual.uri, expected)
	}
}

func TestClient(t *testing.T) {
	if !shouldrun() {
		return
	}
	client := NewClient(uri())
	defer client.Close()
	exchange := "jex_tests"
	key := "tests"
	client.SetupPublishing("jex_tests")

	actual := ""
	expected := "this is a test"
	coord := make(chan int)

	handler := func(d amqp.Delivery) {
		d.Ack(false)
		actual = string(d.Body)
		coord <- 1
	}
	client.AddConsumer(exchange, "test_queue", key, handler)
	go client.Listen()
	time.Sleep(100 * time.Millisecond)
	client.Publish(key, []byte(expected))
	<-coord
	if actual != expected {
		t.Errorf("Handler received %s instead of %s", actual, expected)
	}

}
