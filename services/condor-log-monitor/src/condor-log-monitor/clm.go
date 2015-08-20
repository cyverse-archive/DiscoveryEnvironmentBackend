// condor-log-monitor
//
// Tails the configured EVENT_LOG on a condor submission node, parses events from it,
// and pushes events out to an AMQP broker.
//
// condor-log-monitor will make an attempt at detecting rolling over log files and
// recovering from extended downtime, but whether or not a full recovery is possible
// depends on how the Condor logging is configured. It is still possible to lose
// messages if condor-log-monitor is down for a while and Condor rotates files
// out too many times.
//
// condor-log-monitor attempts to recover from downtime by recording a tombstone file that
// records the inode number, last modified date, processing date, and last
// processed position. At start up clm will look for the tombstoned file and will
// attempt to start processing from that point forward.
//
// If the inode of the new file doesn't match the inode contained in the tombstone,
// then scan the directory for all of the old log files and collect their inodes
// and last modified dates. Sort the old log files from oldest to newest -- based
// on the last modified date -- and iterate through them. Find the file that matches
// the inode of the file from the tombstone and process it starting from the position
// recorded in the tombstone. Then, process all of remaining files until you reach
// the current log file. Process the current log file and record a new tombstone.
// Do not delete the old tombstone until you're ready to record a new one.
//
// If condor-log-monitor has been down for so long that the tombstoned log file no
// longer exists, process all of the log file in order from oldest to newest. Record
// a new tombstone when you reach the end of the newest log file.

package main

import (
	"bufio"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"log"
	"math/rand"
	"os"
	"path"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/streadway/amqp"
)

var (
	cfgPath = flag.String("config", "", "Path to the config file.")
	logPath = flag.String("event-log", "", "Path to the log file.")
	version = flag.Bool("version", false, "Print version information.")
)

var (
	gitref  string
	appver  string
	builtby string
	logger  *log.Logger
)

// TombstonePath is the path to the tombstone file.
const TombstonePath = "/tmp/condor-log-monitor.tombstone"

// LoggerFunc adapts a function so it can be used as an io.Writer.
type LoggerFunc func([]byte) (int, error)

func (l LoggerFunc) Write(logbuf []byte) (n int, err error) {
	return l(logbuf)
}

// LogMessage represents a message that will be logged in JSON format.
type LogMessage struct {
	Service  string `json:"service"`
	Artifact string `json:"art-id"`
	Group    string `json:"group-id"`
	Level    string `json:"level"`
	Time     int64  `json:"timeMillis"`
	Message  string `json:"message"`
}

// NewLogMessage returns a pointer to a new instance of LogMessage.
func NewLogMessage(message string) *LogMessage {
	lm := &LogMessage{
		Service:  "condor-log-monitor",
		Artifact: "condor-log-monitor",
		Group:    "org.iplantc",
		Level:    "INFO",
		Time:     time.Now().UnixNano() / int64(time.Millisecond),
		Message:  message,
	}
	return lm
}

// LogWriter writes to stdout with a custom timestamp.
func LogWriter(logbuf []byte) (n int, err error) {
	m := NewLogMessage(string(logbuf[:]))
	j, err := json.Marshal(m)
	if err != nil {
		return 0, err
	}
	j = append(j, []byte("\n")...)
	return os.Stdout.Write(j)
}

func init() {
	logger = log.New(LoggerFunc(LogWriter), "", 0)
	flag.Parse()
}

// Configuration contains the setting read from a config file.
type Configuration struct {
	EventLog                               string
	AMQPUserPass                           string
	AMQPHost                               string
	ExchangeName, ExchangeType, RoutingKey string
	Durable, Autodelete, Internal, NoWait  bool
}

// ReadConfig reads JSON from 'path' and returns a pointer to a Configuration
// instance. Hopefully.
func ReadConfig(path string) (*Configuration, error) {
	fileInfo, err := os.Stat(path)
	if err != nil {
		return nil, err
	}
	if fileInfo.IsDir() {
		return nil, fmt.Errorf("%s is a directory", path)
	}
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	fileData, err := ioutil.ReadAll(file)
	if err != nil {
		return nil, err
	}
	var config Configuration
	err = json.Unmarshal(fileData, &config)
	if err != nil {
		return &config, err
	}
	return &config, nil
}

// AMQPPublisher contains the state information for a connection to an AMQP
// broker that is capable of publishing data to an exchange.
type AMQPPublisher struct {
	UserPass     string
	Host         string
	ExchangeName string
	ExchangeType string
	RoutingKey   string
	Durable      bool
	Autodelete   bool
	Internal     bool
	NoWait       bool
	connection   *amqp.Connection
	channel      *amqp.Channel
}

// NewAMQPPublisher creates a new instance of AMQPPublisher and returns a
// pointer to it. The connection is not established at this point.
func NewAMQPPublisher(cfg *Configuration) *AMQPPublisher {
	return &AMQPPublisher{
		UserPass:     cfg.AMQPUserPass,
		Host:         cfg.AMQPHost,
		ExchangeName: cfg.ExchangeName,
		ExchangeType: cfg.ExchangeType,
		RoutingKey:   cfg.RoutingKey,
		Durable:      cfg.Durable,
		Autodelete:   cfg.Autodelete,
		Internal:     cfg.Internal,
		NoWait:       cfg.NoWait,
	}
}

// ConnectionErrorChan is used to send error channels to goroutines.
type ConnectionErrorChan struct {
	Channel chan *amqp.Error
}

// Connect will attempt to connect to the AMQP broker, create/use the configured
// exchange, and create a new channel. Make sure you call the Close method when
// you are done, most likely with a defer statement.
func (p *AMQPPublisher) Connect(errorChan chan ConnectionErrorChan) error {
	logger.Printf("Dialing amqp://%s/", p.Host)
	connection, err := amqp.Dial(strings.Join([]string{"amqp://", p.UserPass, "@", p.Host, "/"}, ""))
	if err != nil {
		return err
	}
	p.connection = connection

	logger.Println("Creating channel on the connection.")
	channel, err := p.connection.Channel()
	if err != nil {
		return err
	}
	logger.Printf("Done creating channel on the connection.")

	logger.Printf("Declaring exchange %s with a type of %s", p.ExchangeName, p.ExchangeType)
	err = channel.ExchangeDeclare(
		p.ExchangeName,
		p.ExchangeType,
		p.Durable,
		p.Autodelete,
		p.Internal,
		p.NoWait,
		nil, //arguments
	)
	if err != nil {
		return err
	}
	logger.Println("Done declaring exchange.")
	p.channel = channel
	errors := p.connection.NotifyClose(make(chan *amqp.Error))
	msg := ConnectionErrorChan{
		Channel: errors,
	}
	errorChan <- msg
	return nil
}

// SetupReconnection fires up a goroutine that listens for Close() errors and
// reconnects to the AMQP server if they're encountered.
func (p *AMQPPublisher) SetupReconnection(errorChan chan ConnectionErrorChan) {
	//errors := p.connection.NotifyClose(make(chan *amqp.Error))
	go func() {
		msg := <-errorChan      //msg is sent from the Connect() function
		exitChan := msg.Channel //This is the channel that error notifications will come over.
		for {
			select {
			case exitError, ok := <-exitChan:
				if !ok {
					logger.Println("Exit channel closed.")
				}
				logger.Println(exitError)
				logger.Println("An error was detected with the AMQP connection.")
				logger.Println("condor-log-monitor could be in an inconsistent state.")
				logger.Println("Removing /tmp/condor-log-monitor.tombstone...")
				_, err := os.Stat(TombstonePath)
				if err != nil {
					logger.Printf("Failed to stat %s, could not remove it.", TombstonePath)
				} else {
					err = os.Remove(TombstonePath)
					if err != nil {
						logger.Printf("Error removing %s: \n %s", TombstonePath, err)
					} else {
						logger.Printf("Done removing %s", TombstonePath)
						logger.Println("condor-log-monitor will process the entire log when it restarts")
					}
				}
				logger.Println("Exiting with a -1000 exit code")
				os.Exit(-1000)
			}
		}
	}()
}

// PublishString sends the body off to the configured AMQP exchange.
func (p *AMQPPublisher) PublishString(body string) error {
	return p.PublishBytes([]byte(body))
}

// PublishBytes sends off the bytes to the AMQP broker.
func (p *AMQPPublisher) PublishBytes(body []byte) error {
	log.Printf("Publishing message to the %s exchange using routing key %s", p.ExchangeName, p.RoutingKey)
	if err := p.channel.Publish(
		p.ExchangeName,
		p.RoutingKey,
		false, //mandatory?
		false, //immediate?
		amqp.Publishing{
			Headers:         amqp.Table{},
			ContentType:     "text/plain",
			ContentEncoding: "",
			Body:            body,
			DeliveryMode:    amqp.Transient,
			Priority:        0,
		},
	); err != nil {
		return err
	}
	logger.Printf("Done publishing message.")
	return nil
}

// Close calls Close() on the underlying AMQP connection.
func (p *AMQPPublisher) Close() {
	p.connection.Close()
}

// PublishableEvent is a type that contains the information that gets sent to
// the AMQP broker. It's meant to be marshalled into JSON or some other format.
type PublishableEvent struct {
	Event string
	Hash  string
}

// NewPublishableEvent creates returns a pointer to a newly created instance
// of PublishableEvent.
func NewPublishableEvent(event string) *PublishableEvent {
	hashBytes := sha256.Sum256([]byte(event))
	return &PublishableEvent{
		Event: event,
		Hash:  hex.EncodeToString(hashBytes[:]),
	}
}

// ParseEventFile parses an entire file and sends it to the AMQP broker.
func ParseEventFile(
	filepath string,
	seekTo int64,
	pub *AMQPPublisher,
	setTombstone bool,
) (int64, error) {
	startRegex := "^[\\d][\\d][\\d]\\s.*"
	endRegex := "^\\.\\.\\..*"
	foundStart := false
	var eventlines string //accumulates lines in an event entry

	openFile, err := os.Open(filepath)
	if err != nil {
		return -1, err
	}

	fileStat, err := openFile.Stat()
	if err != nil {
		return -1, err
	}

	if seekTo > fileStat.Size() {
		seekTo = 0
	}

	_, err = openFile.Seek(seekTo, os.SEEK_SET)
	if err != nil {
		return -1, err
	}

	var prefixBuffer []byte // used when the reader gets a partial line
	reader := bufio.NewReader(openFile)
	for {
		line, prefix, err := reader.ReadLine()
		if err != nil {
			break
		}
		// A partial line was read from the file, so store the partial in the buffer
		// and skip the rest of the loop. Don't want to try and parse a partial line
		if prefix {
			// it's possible to get multiple partials in a row if a line is really
			// long, so we need to concat multiple partials together as we go. If we
			// don't, we risk losing data.
			if len(prefixBuffer) > 0 {
				prefixBuffer = append(prefixBuffer, line...)
			} else {
				prefixBuffer = line
			}

			//It's possible to get a partial containing only a "..." which causes the
			//clm to halt until more data is detected in the log file. This can cause
			//a significant delay in sending out a notification.
			if !strings.HasPrefix(string(prefixBuffer), "...") {
				continue
			}
		}
		// if we get here, prefix was false and either the rest of a line was read or
		// the entire line was read at once. if there is data in the prefixBuffer,
		// then partials were previously read and need to be prepended onto the
		// the current chunk stored in 'line'.
		if len(prefixBuffer) > 0 {
			line = append(prefixBuffer, line...)
		}
		prefixBuffer = []byte{} //reset the prefixBuffer for later iterations
		text := string(line[:]) //we need to operate on strings for the regexes.
		if !foundStart {
			matchedStart, err := regexp.MatchString(startRegex, text)
			if err != nil {
				return -1, err
			}
			if matchedStart {
				foundStart = true
				eventlines = eventlines + text + "\n"
				if err != nil {
					return -1, err
				}
			}
		} else {
			matchedEnd, err := regexp.MatchString(endRegex, text)
			if err != nil {
				return -1, err
			}
			eventlines = eventlines + text + "\n"
			if matchedEnd {
				logger.Println(eventlines)
				pubEvent := NewPublishableEvent(eventlines)
				pubJSON, err := json.Marshal(pubEvent)
				if err != nil {
					return -1, err
				}
				if err = pub.PublishBytes(pubJSON); err != nil {
					logger.Println(err)
				}
				eventlines = ""
				foundStart = false
			}
		}

		if setTombstone {
			// we only want to record the tombstone when we're done parsing the file.
			newTombstone, err := NewTombstoneFromFile(openFile)
			if err != nil {
				logger.Printf("Error creating new tombstone: %s\n", err)
				return -1, err
			}
			err = newTombstone.WriteToFile()
			if err != nil {
				logger.Printf("Failed to write tombstone to %s\n", TombstonePath)
				logger.Println(err)
			}
		}
	}
	currentPos, err := openFile.Seek(0, os.SEEK_CUR)
	return currentPos, err
}

// MonitorPath spawns a gorouting that will check the last modified date on the
// file specified by path and attempt to parse it when the date changes.
func MonitorPath(path string, sleepyTime time.Duration, changeDetected chan<- int) error {
	logger.Printf("Monitoring path %s every %s\n", path, sleepyTime.String())

	openFile, err := os.Open(path)
	if err != nil {
		return err
	}

	fileinfo, err := openFile.Stat()
	if err != nil {
		return err
	}

	lastmod := fileinfo.ModTime()
	err = openFile.Close()
	if err != nil {
		return err
	}

	for {
		time.Sleep(sleepyTime)
		openFile, err = os.Open(path)
		if err != nil {
			logger.Println(err)
			continue
		}

		latestInfo, err := openFile.Stat()
		if err != nil {
			logger.Println(err)
			continue
		}

		latestLastMod := latestInfo.ModTime()
		err = openFile.Close()
		if err != nil {
			logger.Println(err)
			continue
		}

		if !latestLastMod.Equal(lastmod) {
			logger.Printf("Change detected in %s\n", path)
			changeDetected <- 1
			lastmod = latestLastMod
		}
	}
}

//TombstoneAction denotes the kind of action a TombstoneMsg represents.
type TombstoneAction int

const (
	//Set says that the TombstoneMsg contains a set action.
	Set TombstoneAction = iota

	//Get says that the TombstoneMsg contains a get action.
	Get

	//Quit says that the TombstoneMsg contains a quit action.
	Quit
)

//TombstoneMsg represents a message sent to a goroutine that processes tombstone
//related operations. The Data field contains information that the tombstone
//goroutine may take action on, depending on the Action. Set messages will set
//the current value of the tombstone to the value in the Data field. Get messages
//will return the current value of the tombstone on the Reply channel. Quit
//messages tell the goroutine to shut down as cleanly as possible. The Reply
//channel may be used on certain operations to pass back data from the goroutine
//in response to a received TombstoneMsg.
type TombstoneMsg struct {
	Action TombstoneAction
	Data   Tombstone
	Reply  chan interface{}
}

// Tombstone is a type that contains the information stored in a tombstone file.
// It tracks the current position, last modified data, and inode number of the
// log file that was parsed and the date that the tombstone was created.
type Tombstone struct {
	CurrentPos int64
	Date       time.Time
	LogLastMod time.Time
	Inode      uint64
}

// TombstoneExists returns true if the tombstone file is present.
func TombstoneExists() bool {
	_, err := os.Stat(TombstonePath)
	if err != nil {
		return false
	}
	return true
}

// InodeFromPath will return the inode number for the given path.
func InodeFromPath(path string) (uint64, error) {
	openFile, err := os.Open(path)
	if err != nil {
		return 0, err
	}
	ino, err := InodeFromFile(openFile)
	if err != nil {
		return 0, err
	}
	return ino, nil
}

// InodeFromFile will return the inode number for the opened file.
func InodeFromFile(openFile *os.File) (uint64, error) {
	fileInfo, err := openFile.Stat()
	if err != nil {
		return 0, err
	}
	sys := fileInfo.Sys().(*syscall.Stat_t)
	return sys.Ino, nil
}

// InodeFromFileInfo will return the inode number from the provided FileInfo
// instance.
func InodeFromFileInfo(info *os.FileInfo) uint64 {
	i := *info
	sys := i.Sys().(*syscall.Stat_t)
	return sys.Ino
}

// NewTombstoneFromPath will create a *Tombstone for the provided path.
func NewTombstoneFromPath(path string) (*Tombstone, error) {
	openFile, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	tombstone, err := NewTombstoneFromFile(openFile)
	if err != nil {
		return nil, err
	}
	return tombstone, nil
}

// NewTombstoneFromFile will create a *Tombstone from an open file.
func NewTombstoneFromFile(openFile *os.File) (*Tombstone, error) {
	fileInfo, err := openFile.Stat()
	if err != nil {
		return nil, err
	}
	inode, err := InodeFromFile(openFile)
	if err != nil {
		return nil, err
	}
	currentPos, err := openFile.Seek(0, os.SEEK_CUR)
	if err != nil {
		return nil, err
	}
	tombstone := &Tombstone{
		CurrentPos: currentPos,
		Date:       time.Now(),
		LogLastMod: fileInfo.ModTime(),
		Inode:      inode,
	}
	return tombstone, nil
}

// WriteToFile will persist the Tombstone to a file.
func (t *Tombstone) WriteToFile() error {
	tombstoneJSON, err := json.Marshal(t)
	if err != nil {
		return err
	}
	err = ioutil.WriteFile(TombstonePath, tombstoneJSON, 0644)
	return err
}

// UnmodifiedTombstone is the tombstone as it was read from the JSON in the
// tombstone file. It hasn't been turned into an actual Tombstone instance yet
// because some of the fields need to be manually converted to a different type.
type UnmodifiedTombstone struct {
	CurrentPos int64
	Date       string
	LogLastMod string
	Inode      uint64
}

// Convert returns a *Tombstone based on the values contained in the
// UnmodifiedTombstone.
func (u *UnmodifiedTombstone) Convert() (*Tombstone, error) {
	parsedDate, err := time.Parse(time.RFC3339Nano, u.Date)
	if err != nil {
		return nil, err
	}
	parsedLogLastMod, err := time.Parse(time.RFC3339, u.LogLastMod)
	if err != nil {
		return nil, err
	}
	tombstone := &Tombstone{
		CurrentPos: u.CurrentPos,
		Date:       parsedDate,
		LogLastMod: parsedLogLastMod,
		Inode:      u.Inode,
	}
	return tombstone, nil
}

// ReadTombstone will read a marshalled tombstone from a file and return a
// pointer to it.
func ReadTombstone() (*Tombstone, error) {
	contents, err := ioutil.ReadFile(TombstonePath)
	if err != nil {
		return nil, err
	}
	var t *UnmodifiedTombstone
	err = json.Unmarshal(contents, &t)
	if err != nil {
		return nil, err
	}
	tombstone, err := t.Convert()
	if err != nil {
		return nil, err
	}
	return tombstone, nil
}

// Logfile contains a pointer to a os.FileInfo instance and the base directory
// for a particular log file.
type Logfile struct {
	Info    os.FileInfo
	BaseDir string
}

// LogfileList contains a list of Logfiles.
type LogfileList []Logfile

// NewLogfileList returns a list of FileInfo instances to files that start with
// the name of the configured log file.
func NewLogfileList(dir string, logname string) (LogfileList, error) {
	startingList, err := ioutil.ReadDir(dir)
	if err != nil {
		return nil, err
	}
	var filtered []Logfile
	for _, fi := range startingList {
		if strings.HasPrefix(fi.Name(), logname) {
			lf := Logfile{
				Info:    fi,
				BaseDir: dir,
			}
			filtered = append(filtered, lf)
		}
	}
	filtered = LogfileList(filtered)
	return filtered, nil
}

func (l LogfileList) Len() int {
	return len(l)
}

func (l LogfileList) Swap(i, j int) {
	l[i], l[j] = l[j], l[i]
}

func (l LogfileList) Less(i, j int) bool {
	re := regexp.MustCompile("\\.\\d+$") //extracts the suffix with a leading '.'
	logfile1 := l[i].Info
	logfile2 := l[j].Info
	logname1 := logfile1.Name()
	logname2 := logfile2.Name()
	match1 := re.Find([]byte(logname1))
	match2 := re.Find([]byte(logname2))

	//filenames without a suffix are effectively equal
	if match1 == nil && match2 == nil {
		return false
	}

	//filenames without a suffix have a lower value than basically anything.
	//this means that the most current log file will get processed last if
	//the monitor has been down for a while.
	if match1 == nil && match2 != nil {
		return false
	}

	//again, filenames without a suffix have a lower value than files with a
	//suffix.
	if match1 != nil && match2 == nil {
		return true
	}

	//the suffix is assumed to be a number. if it's not it has a lower value.
	match1int, err := strconv.Atoi(string(match1[1:])) //have to drop the '.'
	if err != nil {
		return false
	}

	//the suffix is assumed to be a number again. if it doesn't it's assumed to
	//have a lower value.
	match2int, err := strconv.Atoi(string(match2[1:])) //have to drop the '.'
	if err != nil {
		return true
	}

	return match1int > match2int
}

// SliceByInode trims the LogfileList by looking for the log file that has the
// matching inode and returning a list of log files that starts at that point.
func (l LogfileList) SliceByInode(inode uint64) LogfileList {
	foundIdx := 0
	for idx, logfile := range l {
		fiInode := InodeFromFileInfo(&logfile.Info)
		if fiInode == inode {
			foundIdx = idx
			break
		}
	}
	return l[foundIdx:]
}

// PathFromInode returns the path to a file from the LogfileList that has the
// same inode as the one that is passed in. If none of the paths match then the
// returned string will be empty.
func (l LogfileList) PathFromInode(inode uint64) string {
	foundIndex := 0
	didFind := false
	for idx, logfile := range l {
		fileInode := InodeFromFileInfo(&logfile.Info)
		if fileInode == inode {
			didFind = true
			foundIndex = idx
			break
		}
	}
	if !didFind {
		return ""
	}
	fileInstance := l[foundIndex]
	return path.Join(fileInstance.BaseDir, fileInstance.Info.Name())
}

// Version prints version information to stdout
func Version() {
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

/*
On start up, look for tombstone and read it if it's present.
List the log files.
Sort the log files.
Trim the list based on the tombstoned inode number.
If the inode is not present in the list, trim the list based on last-modified date.
If all of the files were modified after the recorded last-modified date, then parse
and send all of the files.
After all of the files are parsed, record a new tombstone and tail the latest log file, looking for updates.
*/
func main() {
	if *version {
		Version()
		os.Exit(0)
	}
	if *cfgPath == "" {
		fmt.Printf("--config must be set.")
		os.Exit(-1)
	}
	cfg, err := ReadConfig(*cfgPath)
	if err != nil {
		fmt.Println(err)
	}
	randomizer := rand.New(rand.NewSource(time.Now().UnixNano()))
	errChan := make(chan ConnectionErrorChan)
	pub := NewAMQPPublisher(cfg)
	pub.SetupReconnection(errChan)

	// Handle badness with AMQP at startup.
	for {
		logger.Println("Attempting AMQP connection...")
		err = pub.Connect(errChan)
		if err != nil {
			logger.Println(err)
			waitFor := randomizer.Intn(10)
			logger.Printf("Re-attempting connection in %d seconds", waitFor)
			time.Sleep(time.Duration(waitFor) * time.Second)
		} else {
			logger.Println("Successfully connected to the AMQP broker.")
			break
		}
	}

	// First, we need to read the tombstone file if it exists.
	var tombstone *Tombstone
	if TombstoneExists() {
		logger.Printf("Attempting to read tombstone from %s\n", TombstonePath)
		tombstone, err = ReadTombstone()
		if err != nil {
			logger.Println("Couldn't read Tombstone file.")
			logger.Println(err)
			tombstone = nil
		}
		logger.Printf("Done reading tombstone file from %s\n", TombstonePath)
	} else {
		tombstone = nil
	}

	logDir := filepath.Dir(cfg.EventLog)
	logger.Printf("Log directory: %s\n", logDir)
	logFilename := filepath.Base(cfg.EventLog)
	logger.Printf("Log filename: %s\n", logFilename)

	// Now we need to find all of the rotated out log files and parse them for
	// potentially missed updates.
	logList, err := NewLogfileList(logDir, logFilename)
	if err != nil {
		logger.Println("Couldn't get list of log files.")
		logList = LogfileList{}
	}

	// We need to sort the rotated log files in order from oldest to newest.
	sort.Sort(logList)

	// If there aren't any rotated log files or a tombstone file, then there
	// isn't a reason to truncate the list of rotated log files. Hopefully, we'd
	// trim the list of log files to prevent reprocessing, which could save us
	// a significant amount of time at start up.
	if len(logList) > 0 && tombstone != nil {
		logger.Printf("Slicing log list by inode number %d\n", tombstone.Inode)
		logList = logList.SliceByInode(tombstone.Inode)
	}

	// Iterate through the list of log files, parse them, and ultimately send the
	// events out to the AMQP broker. Skip the latest log file, we'll be handling
	// that further down.
	for _, logFile := range logList {
		if logFile.Info.Name() == logFilename { //the current log file will get parsed later
			continue
		}
		logfilePath := path.Join(logFile.BaseDir, logFile.Info.Name())
		logger.Printf("Parsing %s\n", logfilePath)

		if tombstone != nil {
			logfileInode := InodeFromFileInfo(&logFile.Info)

			// Inodes need to match and the current position needs to be less than the file size.
			if logfileInode == tombstone.Inode && tombstone.CurrentPos < logFile.Info.Size() {
				logger.Printf("Tombstoned inode matches %s, starting parse at %d\n", logfilePath, tombstone.CurrentPos)
				_, err = ParseEventFile(logfilePath, tombstone.CurrentPos, pub, false)
			} else {
				logger.Printf("Tombstoned inode does not match %s, starting parse at position 0\n", logfilePath)
				_, err = ParseEventFile(logfilePath, 0, pub, false)
			}
		} else {
			logger.Printf("No tombstone found, starting parse at position 0 for %s\n", logfilePath)
			_, err = ParseEventFile(logfilePath, 0, pub, false)
		}
		if err != nil {
			logger.Println(err)
		}
	}

	changeDetected := make(chan int)
	var startPos int64

	d, err := time.ParseDuration("0.5s")
	if err != nil {
		logger.Println(err)
	}
	go func() {
		logger.Println("Beginning event log monitor goroutine.")
		// get the ball rolling...
		changeDetected <- 1
		err = MonitorPath(cfg.EventLog, d, changeDetected)
		if err != nil {
			logger.Println(err)
		}
	}()

	for {
		select {
		case <-changeDetected:
			//Get the tombstone if it exists.
			if TombstoneExists() {
				tombstone, err = ReadTombstone()
				if err != nil {
					logger.Println(err)
				}
				startPos = tombstone.CurrentPos

				// Get the path to the file that the Tombstone was indicating
				oldLogs, err := NewLogfileList(logDir, logFilename)
				if err != nil {
					logger.Println(err)
				}

				// If the path to the file is different from the configured file the the
				// log likely rolled over.
				pathFromTombstone := oldLogs.PathFromInode(tombstone.Inode)
				if pathFromTombstone != "" && pathFromTombstone != cfg.EventLog {
					oldInfo, err := os.Stat(pathFromTombstone)
					if err != nil {
						logger.Println(err)
					}
					// Compare the start position to the size of the
					// file. If it's less than the size of the file, more of the old file
					// needs to be parsed.
					if startPos < oldInfo.Size() {
						_, err = ParseEventFile(pathFromTombstone, startPos, pub, true)
						if err != nil {
							logger.Println(err)
						}
						// Afterwards set the startPos to 0 if it isn't
						// already, but ONLY if an old file was parsed first.
						startPos = 0
					}
				}
			} else {
				// The Tombstone didn't exist, so start from the beginning of the file.
				startPos = 0
			}

			logger.Printf("Parsing %s starting at position %d\n", cfg.EventLog, startPos)
			startPos, err = ParseEventFile(cfg.EventLog, startPos, pub, true)
			if err != nil {
				logger.Println(err)
			}
		}
	}
}
