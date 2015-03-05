package main

import (
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"

	fsnotify "gopkg.in/fsnotify.v1"
)

// CondorSubmit submits a job to Condor
func CondorSubmit(filePath string, executable string) error {
	log.Printf("Executing %s on %s", executable, filePath)

	raw, err := ioutil.ReadFile(filePath)
	if err != nil {
		log.Print(err)
		return err
	}
	contents := strings.Split(string(raw[:]), "\n")
	if len(contents) < 2 {
		log.Printf("File %s didn't have two lines", filePath)
		return fmt.Errorf("File %s didn't have two lines", filePath)
	}
	submissionPath := contents[0]
	logFilePath := contents[1]

	log.Printf("Path to submission file is %s", submissionPath)
	log.Printf("Log file path is %s", logFilePath)
	err = os.MkdirAll(logFilePath, 0777)
	if err != nil {
		log.Print(err)
		return err
	}
	csPath, err := exec.LookPath(executable)
	if err != nil {
		log.Print(err)
		return err
	}
	cmd := exec.Command(csPath, submissionPath)
	if err != nil {
		log.Print(err)
		return err
	}
	cmd.Dir = logFilePath
	buff, err := cmd.CombinedOutput()
	fmt.Println(string(buff[:]))
	if err != nil {
		log.Print(err)
		return err
	}
	return nil
}

func CorrectEvent(event fsnotify.Event) bool {
	switch runtime.GOOS {
	case "darwin":
		if event.Op == fsnotify.Chmod {
			return true
		}
		return false
	case "linux":
		if event.Op == fsnotify.Chmod || event.Op == fsnotify.Create {
			return true
		}
		return false
	default:
		return true
	}
}

func main() {
	WatchDir := os.Getenv("CJS_WATCH_DIR")
	if WatchDir == "" {
		log.Println("CJS_WATCH_DIR is not set.")
		os.Exit(-1)
	}

	Executable := os.Getenv("CJS_EXECUTABLE")
	if Executable == "" {
		log.Println("Defaulting CJS_EXECUTABLE to 'condor_submit'")
		Executable = "condor_submit"
	}

	watcher, err := fsnotify.NewWatcher()
	if err != nil {
		log.Fatal(err)
	}
	defer watcher.Close()

	done := make(chan bool)

	go func() {
		for {
			select {
			case event := <-watcher.Events:
				var fullPath string
				if !filepath.IsAbs(event.Name) {
					fullPath, err = filepath.Abs(event.Name)
					if err != nil {
						log.Println(err)
						continue
					}
				} else {
					fullPath = event.Name
				}
				if CorrectEvent(event) && strings.HasSuffix(fullPath, ".submit") {
					fullPathFile, err := os.Open(fullPath)
					if err != nil {
						log.Println(err)
						fullPathFile.Close()
						continue
					}
					fpStat, err := fullPathFile.Stat()
					if err != nil {
						log.Println(err)
						fullPathFile.Close()
						continue
					}
					fpMode := fpStat.Mode()
					if !fpMode.IsDir() {
						log.Printf("Renamed %s", fullPath)
						go CondorSubmit(fullPath, Executable)
					}
					fullPathFile.Close()
				}
				log.Println("event:", event)
			case err := <-watcher.Errors:
				log.Println("error:", err)
			}
		}
	}()

	err = watcher.Add(WatchDir)
	if err != nil {
		log.Fatal(err)
	}

	<-done
}
