package main

import (
	"configurate"
	"database/sql"
	"encoding/json"
	"flag"
	"fmt"
	"logcabin"
	"messaging"
	"net"
	"os"
	"time"

	_ "github.com/lib/pq"
	"github.com/streadway/amqp"
)

var (
	logger     = logcabin.New()
	version    = flag.Bool("version", false, "Print the version information")
	cfgPath    = flag.String("config", "", "The path to the config file")
	gitref     string
	appver     string
	builtby    string
	amqpClient *messaging.Client
	db         *sql.DB
)

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

func insert(state, invID, msg, host, ip string, sentOn time.Time) (sql.Result, error) {
	insertStr := `
INSERT INTO job_status_updates (
  external_id,
  message,
  status,
  sent_from,
  sent_from_hostname,
  sent_on
) VALUES (
  $1,
  $2,
  $3,
  $4,
  $5,
  $6
) RETURNING id`
	return db.Exec(insertStr, invID, msg, state, ip, host, sentOn)
}

func msg(delivery amqp.Delivery) {
	delivery.Ack(false)
	update := &messaging.UpdateMessage{}
	err := json.Unmarshal(delivery.Body, update)
	if err != nil {
		logger.Print(err)
		return
	}
	if update.State == "" {
		logger.Println("State was unset, dropping update")
		return
	}
	if update.Job.InvocationID == "" {
		logger.Println("InvocationID was unset, dropping update")
	}
	if update.Message == "" {
		logger.Println("Message set to empty string, setting to UNKNOWN")
		update.Message = "UNKNOWN"
	}
	var sentFromAddr string
	if update.Sender != "" {
		ips, err := net.LookupIP(update.Sender)
		if err != nil {
			logger.Print(err)
		} else {
			if len(ips) > 0 {
				sentFromAddr = ips[0].String()
			}
		}
	}
	result, err := insert(
		string(update.State),
		update.Job.InvocationID,
		update.Message,
		update.Sender,
		sentFromAddr,
		delivery.Timestamp,
	)
	if err != nil {
		logger.Print(err)
		return
	}
	rowCount, err := result.RowsAffected()
	if err != nil {
		logger.Print(err)
		return
	}
	logger.Printf("Inserted %d rows\n", rowCount)
}

func main() {
	if *version {
		AppVersion()
		os.Exit(0)
	}
	if *cfgPath == "" {
		logger.Fatal("--config must be set.")
	}
	err := configurate.Init(*cfgPath)
	if err != nil {
		logger.Fatal(err)
	}
	dbURI, err := configurate.C.String("db.uri")
	if err != nil {
		logger.Fatal(err)
	}
	amqpURI, err := configurate.C.String("amqp.uri")
	if err != nil {
		logger.Fatal(err)
	}
	amqpClient = messaging.NewClient(amqpURI)
	defer amqpClient.Close()
	db, err = sql.Open("postgres", dbURI)
	if err != nil {
		logger.Fatal(err)
	}
	err = db.Ping()
	if err != nil {
		logger.Fatal(err)
	}
	amqpClient.AddConsumer(messaging.JobsExchange, "job_status_recorder", messaging.UpdatesKey, msg)
	amqpClient.Listen()
}
