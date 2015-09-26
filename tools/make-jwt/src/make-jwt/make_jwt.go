package main

import (
	"flag"
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

func main() {
}
