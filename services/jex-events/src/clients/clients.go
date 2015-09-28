package clients

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
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

// JobRecord returns a JobRecord associated with the uuid that's passed in.
func (j *JEXEventsClient) JobRecord(uuid string) (*model.JobRecord, error) {
	requestPath := path.Join(j.URL.Path, "jobs", uuid)
	if !strings.HasPrefix(requestPath, "/") {
		requestPath = fmt.Sprintf("/%s", requestPath)
	}
	requestURL := fmt.Sprintf("%s://%s%s", j.URL.Scheme, j.URL.Host, requestPath)
	response, err := http.Get(requestURL)
	if err != nil {
		return nil, err
	}
	data, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}
	var jr model.JobRecord
	err = json.Unmarshal(data, &jr)
	if err != nil {
		return nil, err
	}
	return &jr, err
}
