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
	cfgPath = flag.String("config", "", "Path to the config value")
	version = flag.Bool("version", false, "Print the version information")
	gitref  string
	appver  string
	builtby string
	logger  *log.Logger
	args    []string
)

func init() {
	logger = log.New(logcabin.LoggerFunc(logcabin.LogWriter), "", log.Lshortfile)
	flag.Parse()
	args = flag.Args()
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

func help() string {
	h := `
jex-events, services for monitoring DE jobs.

Usage:

	jex-events mode [arguments]

The modes are:

	monitor     Monitor Condor's event_log for changes
	events      Listen for events parsed by monitor
	help        Prints out this help message

Each mode accepts the following options:

	--config    The path to the configuration file. Required.
	--version   Print out version information`
	return h
}

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}

	if len(args) == 0 {
		fmt.Println("No mode specified")
		fmt.Println(help())
		os.Exit(-1)
	}

	if len(args) > 1 {
		fmt.Println("Multiple modes specified")
		fmt.Println(help())
		os.Exit(-1)
	}

	validModes := []string{"monitor", "events", "help"}
	mode := args[0]
	foundMode := false

	for _, v := range validModes {
		if v == mode {
			foundMode = true
		}
	}

	if !foundMode {
		fmt.Printf("Invalid mode: %s\n", mode)
		fmt.Println(help())
		os.Exit(-1)
	}

	if *cfgPath == "" {
		fmt.Println("Error: --config must be set.")
		fmt.Println(help())
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

	switch mode {
	case "help":
		fmt.Println(help())
	case "events":
		events.Run(cfg, logger)
	case "monitor":
		monitor.Run(cfg, logger)
	default:
		fmt.Println("Bad mode! Bad! Look what you did!")
		fmt.Println(help())
		os.Exit(-1)
	}
}
