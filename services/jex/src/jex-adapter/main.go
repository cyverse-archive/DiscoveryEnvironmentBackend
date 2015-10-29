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
	fmt.Fprintf(writer, "Welcome to the JEX.\n")
}

func stop(writer http.ResponseWriter, request *http.Request) {
	logger.Printf("Request received:\n%#v\n", request)
	var (
		invID string
		ok    bool
		err   error
		v     = mux.Vars(request)
	)
	logger.Println("Getting invocation ID out of the Vars")
	if invID, ok = v["invocation_id"]; !ok {
		writer.WriteHeader(http.StatusBadRequest)
		writer.Write([]byte("Missing job id in URL"))
		logger.Print("Missing job id in URL")
		return
	}
	logger.Printf("Invocation ID is %s\n", invID)
	stopRequest := messaging.StopRequest{
		Reason:       "User request",
		Username:     "system",
		InvocationID: invID,
	}
	logger.Println("Marshalling stop request to JSON")
	reqJSON, err := json.Marshal(stopRequest)
	if err != nil {
		logger.Print(err)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(fmt.Sprintf("Error creating stop request JSON: %s", err.Error())))
		return
	}
	logger.Println("Sending stop request")
	stopKey := fmt.Sprintf("%s.%s", messaging.StopsKey, invID)
	err = client.Publish(stopKey, reqJSON)
	if err != nil {
		logger.Print(err)
		writer.WriteHeader(http.StatusInternalServerError)
		writer.Write([]byte(fmt.Sprintf("Error sending stop request: %s", err.Error())))
		return
	}
	logger.Println("Done sending stop request")
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

//PreviewerReturn is what the arg-preview endpoint returns.
type PreviewerReturn struct {
	Params string `json:"params"`
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
	var paramMap PreviewerReturn
	paramMap.Params = previewer.Params.String()
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

// NewRouter returns a newly configured *mux.Router.
func NewRouter() *mux.Router {
	router := mux.NewRouter()
	router.HandleFunc("/", home).Methods("GET")
	router.HandleFunc("/", launch).Methods("POST")
	router.HandleFunc("/stop/{invocation_id}", stop).Methods("DELETE")
	router.HandleFunc("/arg-preview", preview).Methods("POST")
	return router
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
	router := NewRouter()
	log.Fatal(http.ListenAndServe(*addr, router))
}
