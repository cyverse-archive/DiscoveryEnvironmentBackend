package api

import (
	"fmt"
	"logcabin"
	"net/http"
)

var (
	logger = logcabin.New()
)

// RespondWithError logs the error to stdout/stderr using msgTmpl as the template.
// The message is then written to 'w', after setting the status code to http.StatusBadRequest.
func RespondWithError(msgTmpl string, err error, w http.ResponseWriter) {
	msg := fmt.Sprintf(msgTmpl, err)
	logger.Println(msg)
	w.WriteHeader(http.StatusBadRequest)
	w.Write([]byte(msg))
	return
}
