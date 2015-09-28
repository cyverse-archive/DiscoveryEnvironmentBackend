package main

import (
	"configs"
	"flag"
	"fmt"
	"log"
	"os"
	"os/user"
	"path/filepath"
	"strconv"
)

var (
	tokenLifetime = flag.Int("lifetime", 300, "seconds before tokens expire")
	keyPath       = flag.String("key-path", "", "path to private key")
	keyPassword   = flag.String("key-pass", "", "password used to open private key")
	username      = flag.String("username", "", "username to place in the token")
	email         = flag.String("email", "", "email address to place in the token")
	givenName     = flag.String("given-name", "", "given name to place in the token")
	familyName    = flag.String("family-name", "", "family name to place in the token")
	name          = flag.String("name", "", "name to place in the token")
)

func init() {
	flag.Parse()
}

func homeParameterFile() string {
	usr, err := user.Current()
	if err != nil {
		log.Fatal(err)
	}

	return filepath.Join(usr.HomeDir, ".make-jwt")
}

func loadParameterFile(path string) {

	// Open the parameter file.
	paramFile, err := configs.NewParamFile(path)
	if os.IsNotExist(err) {
		return
	}
	if err != nil {
		log.Println(err)
		return
	}
	defer paramFile.Close()

	// Load individual parameters from the file.
	for paramFile.NextParam() {
		paramName, paramValue := paramFile.ExtractParam()
		if *paramName == "lifetime" {
			lifetime, err := strconv.Atoi(*paramValue)
			if err != nil {
				log.Printf("Invalid token lifetime: %s\n", *paramValue)
			} else {
				tokenLifetime = &lifetime
			}
		} else if *paramName == "key-path" {
			keyPath = paramValue
		} else if *paramName == "key-pass" {
			keyPassword = paramValue
		} else if *paramName == "username" {
			username = paramValue
		} else if *paramName == "email" {
			email = paramValue
		} else if *paramName == "given-name" {
			givenName = paramValue
		} else if *paramName == "family-name" {
			familyName = paramValue
		} else if *paramName == "name" {
			name = paramValue
		} else {
			log.Printf("Unrecognized parameter name: %s\n", *paramName)
		}
	}
}

func loadParameterFiles() {
	loadParameterFile(homeParameterFile())
	loadParameterFile(".make-jwt")
}

func main() {
	loadParameterFiles()
	fmt.Printf("key path: %s\n", *keyPath)
	fmt.Printf("key pass: %s\n", *keyPassword)
}
