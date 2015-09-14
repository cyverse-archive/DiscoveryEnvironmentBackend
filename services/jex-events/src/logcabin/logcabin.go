package logcabin

import (
	"encoding/json"
	"os"
	"time"
)

// LoggerFunc adapts a function so it can be used as an io.Writer.
type LoggerFunc func([]byte) (int, error)

func (l LoggerFunc) Write(logbuf []byte) (n int, err error) {
	return l(logbuf)
}

// LogMessage represents a message that will be logged in JSON format.
type LogMessage struct {
	Service  string `json:"service"`
	Artifact string `json:"art-id"`
	Group    string `json:"group-id"`
	Level    string `json:"level"`
	Time     int64  `json:"timeMillis"`
	Message  string `json:"message"`
}

// NewLogMessage returns a pointer to a new instance of LogMessage.
func NewLogMessage(message string) *LogMessage {
	lm := &LogMessage{
		Service:  "jex-events",
		Artifact: "jex-events",
		Group:    "org.iplantc",
		Level:    "INFO",
		Time:     time.Now().UnixNano() / int64(time.Millisecond),
		Message:  message,
	}
	return lm
}

// LogWriter writes to stdout with a custom timestamp.
func LogWriter(logbuf []byte) (n int, err error) {
	m := NewLogMessage(string(logbuf[:]))
	j, err := json.Marshal(m)
	if err != nil {
		return 0, err
	}
	j = append(j, []byte("\n")...)
	return os.Stdout.Write(j)
}
