(ns user-preferences.config
  (:use [korma.db :only [default-connection]])
  (:require [bouncer [core :as b] [validators :as v]]
            [common-cfg.cfg :as cfg]))

(dosync
 (ref-set
  cfg/validators
   {:port        [v/required cfg/intablev]
    :db-host     [v/required cfg/stringv]
    :db-port     [v/required cfg/stringv]
    :db-user     [v/required cfg/stringv]
    :db-name     [v/required cfg/stringv]
    :db-password [v/required cfg/stringv]})

 (ref-set
  cfg/filters
  #{:db-password :db-user})

 (defn subname
  []
  (let [db-host (:db-host @cfg/cfg)
        db-port (:db-port @cfg/cfg)
        db-name (:db-name @cfg/cfg)]
    (str "//" db-host ":" db-port "/" db-name))))

(defn db-spec
  []
  (dosync
   {:subprotocol "postgresql"
    :classname   "org.postgresql.Driver"
    :subname     (subname)
    :user        (:db-user @cfg/cfg)
    :password    (:db-password @cfg/cfg)}))

(defn connect-db
  []
  (default-connection (db-spec)))
