package main

import (
	"bytes"
	"encoding/gob"
	"encoding/json"
	"flag"
	"io/ioutil"
	"log"
	"os"
	"text/template"
)

var templateData interface{}

func templateBytes(tmplInterface interface{}) ([]byte, error) {
	var buf bytes.Buffer
	encoder := gob.NewEncoder(&buf)
	err := encoder.Encode(tmplInterface)
	if err != nil {
		return nil, err
	}
	return buf.Bytes(), nil
}

func main() {
	var dataFilePath = flag.String("data", "", "Path to the file containing data used in the template.")
	var templateFilePath = flag.String("template", "", "Path to the template.")
	flag.Parse()

	if *dataFilePath == "" {
		log.Fatal("--data must be set.")
	}
	if *templateFilePath == "" {
		log.Fatal("--template must be set.")
	}
	dataFile, err := os.Open(*dataFilePath)
	if err != nil {
		log.Fatal(err)
	}
	dataFileContents, err := ioutil.ReadAll(dataFile)
	if err != nil {
		log.Fatal(err)
	}
	tmpl, err := template.ParseFiles(*templateFilePath)
	if err != nil {
		log.Fatal(err)
	}
	err = json.Unmarshal(dataFileContents, &templateData)
	if err != nil {
		log.Fatal(err)
	}
	err = tmpl.Execute(os.Stdout, templateData)
	if err != nil {
		log.Fatal(err)
	}

}
