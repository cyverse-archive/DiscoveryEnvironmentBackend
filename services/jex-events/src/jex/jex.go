package main

import (
	"condor"
	"configurate"
	"flag"
	"fmt"
	"logcabin"
	"manager"
	"os"
)

var (
	cfgPath = flag.String("config", "", "Path to the config value. Required.")
	mode    = flag.String("mode", "", "One of 'manager', or 'condor-launcher'. Required.")
	version = flag.Bool("version", false, "Print the version information")
	gitref  string
	appver  string
	builtby string
	logger  *logcabin.Lincoln
)

func init() {
	logger = logcabin.New()
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

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}
	validModes := []string{"manager", "condor-launcher"}
	foundMode := false
	for _, v := range validModes {
		if v == *mode {
			foundMode = true
		}
	}
	if !foundMode {
		fmt.Printf("Invalid mode: %s\n", *mode)
		flag.PrintDefaults()
		os.Exit(-1)
	}
	if *cfgPath == "" {
		fmt.Println("Error: --config must be set.")
		flag.PrintDefaults()
		os.Exit(-1)
	}
	err := configurate.Init(*cfgPath)
	if err != nil {
		logger.Print(err)
		os.Exit(-1)
	}
	logger.Println("Done reading config.")
	// if !configurate.Config.Valid() {
	// 	logger.Println("Something is wrong with the config file.")
	// 	os.Exit(-1)
	// }
	switch *mode {
	case "manager":
		manager.Run()
	case "condor-launcher":
		condor.Run()
	default:
		fmt.Println("Bad mode! Bad! Look what you did!")
		flag.PrintDefaults()
		os.Exit(-1)
	}
}
