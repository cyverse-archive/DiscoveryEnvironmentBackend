package launcher

import (
	"configurate"
	"fmt"
	"jexapi"
	"logcabin"
	"strings"
	"submissions"

	"github.com/codegangsta/negroni"
)

// Run starts up the JEX in 'launcher' mode, which launches jobs in response
// to job requests that come through the HTTP/JSON API.
func Run(config *configurate.Configuration, l *logcabin.Lincoln) {
	submissions.Init(config, l)
	router := jexapi.Start(config, l)
	n := negroni.New(l)
	n.UseHandler(router)
	port := config.JEXListenPort
	l.Printf("launcher listening on port %s", port)
	if !strings.HasPrefix(port, ":") {
		port = fmt.Sprintf(":%s", port)
	}
	n.Run(port)
}
