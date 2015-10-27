package main

import (
	"configurate"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"logcabin"
	"messaging"
	"model"
	"net/http"
	"os"

	"github.com/gorilla/mux"
)

var (
	logger     = logcabin.New()
	version    = flag.Bool("version", false, "Print version information")
	configPath = flag.String("config", "", "Path to the configuration file")
	amqpURI    = flag.String("amqp", "", "The amqp:// URI for the broker to connect to")
	addr       = flag.String("addr", ":60000", "The port to listen on for HTTP requests")
	gitref     string
	appver     string
	builtby    string
	client     *messaging.Client
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
	var (
		invID string
		ok    bool
		err   error
		v     = mux.Vars(request)
	)
	if invID, ok = v["invocation_id"]; !ok {
		writer.WriteHeader(http.StatusBadRequest)
		writer.Write([]byte("Missing job id in URL"))
		log.Print("Missing job id in URL")
		return
	}
	stopRequest := messaging.StopRequest{
		Reason:       "User request",
		Username:     "system",
		InvocationID: invID,
	}
	reqJSON, err := json.Marshal(stopRequest)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(fmt.Sprintf("Error creating stop request JSON: %s", err.Error())))
		return
	}
	err = client.Publish(messaging.StopsKey, reqJSON)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(fmt.Sprintf("Error sending stop request: %s", err.Error())))
		return
	}
}

func launch(writer http.ResponseWriter, request *http.Request) {
	bodyBytes, err := ioutil.ReadAll(request.Body)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusBadRequest)
		writer.Write([]byte("Request had no body"))
		return
	}
	job, err := model.NewFromData(bodyBytes)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusBadRequest)
		writer.Write([]byte(fmt.Sprintf("Failed to create job from json: %s", err.Error())))
		return
	}
	launchRequest := messaging.NewLaunchRequest(job)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(fmt.Sprintf("Error creating launch request: %s", err.Error())))
		return
	}
	launchJSON, err := json.Marshal(launchRequest)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(fmt.Sprintf("Error creating launch request JSON: %s", err.Error())))
		return
	}
	err = client.Publish(messaging.LaunchesKey, launchJSON)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(fmt.Sprintf("Error publishing launch request: %s", err.Error())))
		return
	}
}

//Previewer contains a list of params that need to be constructed into a
//command-line preview.
type Previewer struct {
	Params model.PreviewableStepParam `json:"params"`
}

// Preview returns the command-line preview as a string.
func (p *Previewer) Preview() string {
	return p.Params.String()
}

func preview(writer http.ResponseWriter, request *http.Request) {
	bodyBytes, err := ioutil.ReadAll(request.Body)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusBadRequest)
		writer.Write([]byte("Request had no body"))
		return
	}
	previewer := &Previewer{}
	err = json.Unmarshal(bodyBytes, previewer)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusBadRequest)
		writer.Write([]byte(fmt.Sprintf("Error parsing preview JSON: %s", err.Error())))
		return
	}
	var paramMap map[string]string
	paramMap["params"] = previewer.Params.String()
	outgoingJSON, err := json.Marshal(paramMap)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(fmt.Sprintf("Error creating response JSON: %s", err.Error())))
		return
	}
	_, err = writer.Write(outgoingJSON)
	if err != nil {
		log.Print(err)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(fmt.Sprintf("Error writing response: %s", err.Error())))
		return
	}
}

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}
	if *amqpURI == "" {
		log.Fatal("--amqp is required")
	}
	if *configPath == "" {
		log.Fatal("--config is required")
	}
	err := configurate.Init(*configPath)
	if err != nil {
		log.Fatal(err)
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
