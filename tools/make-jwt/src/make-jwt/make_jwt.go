package main

import (
	"bufio"
	"flag"
	"fmt"
	"log"
	"os/user"
	"path/filepath"
	"strings"
)

var (
	tokenLifetime = flag.Int("lifetime", 300, "seconds before tokens expire")
	keyPath       = flag.String("key-path", "", "path to private key")
	keyPassword   = flag.String("key-pass", "", "password used to open private key")
	username      = flag.String("username", "", "username to place in the token")
	email         = flag.String("email", "", "email address to place in the token")
	givenName     = flag.String("givenName", "", "given name to place in the token")
	familyName    = flag.String("familyName", "", "family name to place in the token")
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
	f, err := os.Open(path)
	if err == os.ErrNotExist {
		return
	}
	if err != nil {
		log.Warn(err)
	}

	defer f.Close()

	scanner := bufio.NewScanner(f)
	for (scanner.Scan()) {
		strs := strings.SplitN(Scanner.Text(), "\\s*=\\s*", 2)
	}
}

func loadParameterFiles() {
	loadParameterFile(homeParameterFile())
	loadParameterFile(".make-jwt")
}

func main() {
	loadParameterFiles()
}
