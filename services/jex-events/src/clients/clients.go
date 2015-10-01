package clients

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"model"
	"net/http"
	"net/url"
	"path"
	"strings"
)

// JEXEventsClient is an HTTP client for the jex-events service
type JEXEventsClient struct {
	URL *url.URL
}

// NewJEXEventsClient returns a pointer to an initialized JEXEventsClient
func NewJEXEventsClient(serviceURL string) (*JEXEventsClient, error) {
	parsedURL, err := url.Parse(serviceURL)
	if err != nil {
		return nil, err
	}
	return &JEXEventsClient{URL: parsedURL}, nil
}

// JobRecord returns a Job associated with the uuid that's passed in.
func (j *JEXEventsClient) JobRecord(uuid string) (*model.Job, error) {
	requestPath := path.Join(j.URL.Path, "invocations", uuid)
	if !strings.HasPrefix(requestPath, "/") {
		requestPath = fmt.Sprintf("/%s", requestPath)
	}
	requestURL := fmt.Sprintf("%s://%s%s", j.URL.Scheme, j.URL.Host, requestPath)
	log.Printf("JobRecord URL: %s\n", requestURL)
	response, err := http.Get(requestURL)
	if err != nil {
		return nil, err
	}
	if response.StatusCode != 200 {
		return nil, fmt.Errorf("A status code of %d was returned by jex-events", response.StatusCode)
	}
	data, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}
	log.Printf("Data was:\n%s", string(data))
	var jr model.Job
	err = json.Unmarshal(data, &jr)
	if err != nil {
		return nil, err
	}
	return &jr, err
}
