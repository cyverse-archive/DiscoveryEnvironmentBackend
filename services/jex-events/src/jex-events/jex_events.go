package main

import (
	"configurate"
	"events"
	"flag"
	"fmt"
	"log"
	"logcabin"
	"monitor"
	"os"
)

var (
	cfgPath = flag.String("config", "", "Path to the config value. Required.")
	mode    = flag.String("mode", "", "One of 'monitor' or 'events'. Required.")
	version = flag.Bool("version", false, "Print the version information")
	gitref  string
	appver  string
	builtby string
	logger  *log.Logger
)

func init() {
	logger = log.New(logcabin.LoggerFunc(logcabin.LogWriter), "", log.Lshortfile)
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

	validModes := []string{"monitor", "events"}
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

	cfg, err := configurate.New(*cfgPath, logger)
	if err != nil {
		logger.Print(err)
		os.Exit(-1)
	}

	logger.Println("Done reading config.")
	if !cfg.Valid() {
		logger.Println("Something is wrong with the config file.")
		os.Exit(-1)
	}

	switch *mode {
	case "events":
		events.Run(cfg, logger)
	case "monitor":
		monitor.Run(cfg, logger)
	default:
		fmt.Println("Bad mode! Bad! Look what you did!")
		flag.PrintDefaults()
		os.Exit(-1)
	}
}
