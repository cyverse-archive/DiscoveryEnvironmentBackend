package launcher

import (
	"api"
	"configurate"
	"fmt"
	"logcabin"
	"strings"

	"github.com/codegangsta/negroni"
)

// Run starts up the JEX in 'launcher' mode, which launches jobs in response
// to job requests that come through the HTTP/JSON API.
func Run(config *configurate.Configuration, l *logcabin.Lincoln) {
	//model.Init(config, l)
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
