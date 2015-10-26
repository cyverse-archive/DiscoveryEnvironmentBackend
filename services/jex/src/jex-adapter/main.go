package main

import (
	"flag"
	"fmt"
	"log"
	"logcabin"
	"messaging"
	"net/http"
	"os"

	"github.com/gorilla/mux"
)

var (
	logger  = logcabin.New()
	version = flag.Bool("version", false, "Print version information")
	amqpURI = flag.String("amqp", "", "The amqp:// URI for the broker to connect to")
	addr    = flag.String("addr", ":60000", "The port to listen on for HTTP requests")
	gitref  string
	appver  string
	builtby string
	client  *messaging.Client
)

func init() {
	flag.Parse()
}

// AppVersion prints version information to stdout
func AppVersion() {
	if appver != "" {
		fmt.Printf("App-Version: %s\n", appver)
	}
	if gitref != "" {
		fmt.Printf("Git-Ref: %s\n", gitref)
	}

	if builtby != "" {
		fmt.Printf("Built-By: %s\n", builtby)
	}
}

func home(writer http.ResponseWriter, request *http.Request) {
	fmt.Fprintf(writer, "Welcome to the JEX.")
}

func stop(writer http.ResponseWriter, request *http.Request) {
}

func launch(writer http.ResponseWriter, request *http.Request) {

}

func preview(writer http.ResponseWriter, request *http.Request) {

}

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}
	if *amqpURI == "" {
		log.Fatal("--amqp is required")
	}
	client = messaging.NewClient(*amqpURI)
	defer client.Close()
	client.SetupPublishing(messaging.JobsExchange)
	router := mux.NewRouter()
	router.HandleFunc("/", home).Methods("GET")
	router.HandleFunc("/", launch).Methods("POST")
	router.HandleFunc("/stop/{invocation_id}", stop).Methods("DELETE")
	router.HandleFunc("/arg-preview", preview).Methods("POST")
	log.Fatal(http.ListenAndServe(*addr, router))
}
