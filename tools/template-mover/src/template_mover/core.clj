(ns template-mover.core
  (:require [clojure.pprint :refer [pprint]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all]
            [kameleon.pgpass :as pgpass]))

(def ^:private cli-options
  [["-h" "--host HOST" "Database host name"
    :default "localhost"
    :validate [(complement string/blank?) "Must not be blank."]]
   ["-p" "--port PORT" "Database port number"
    :default 5432
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 65536) "Must be an integer between 0 and 65536."]]
   ["-U" "--user USER" "Database username"
    :default "de"
    :validate [(complement string/blank?) "Must not be blank"]]
   ["-d" "--de-database DATABASE" "DE database name"
    :default "de"
    :validate [(complement string/blank?) "Must not be blank"]]
   ["-m" "--metadata-database DATABASE" "Metadata database name"
    :default "metadata"
    :validate [(complement string/blank?) "Must not be blank"]]
   ["-?" "--help" "Display this help message"]])

(defn- bad-usage
  [summary errors]
  (println "Invalid Usage:")
  (dorun (map (comp println (partial str "\t")) errors))
  (println)
  (println summary)
  (System/exit 1))

(defn- show-help
  [summary]
  (println summary)
  (System/exit 0))

(defn- prompt-for-password
  "Prompts the user for a password."
  [user]
  (print user "password: ")
  (flush)
  (.. System console readPassword))

(defn- get-password
  "Attempts to obtain the database password from the user's .pgpass file.  If
  the password can't be obtained from .pgpass, prompts the user for the
  password"
  [host port database user]
  (let [password (pgpass/get-password host port database user)]
    (if (nil? password)
      (if (nil? (System/console))
        (binding [*out* *err*]
          (println "No password supplied.")
          (System/exit 1))
        (prompt-for-password user))
      password)))

(defn- dbconnect
  [host port db user password]
  (->> {:classname   "org.postgresql.Driver"
        :subprotocol "postgresql"
        :subname     (str "//" host ":" port "/" db)
        :user        user
        :password    password}
       (jdbc/get-connection)))

(defn- run-query
  [db sql-map]
  (jdbc/query db (sql/format sql-map)))

(defn- migrate-metadata-template-tables
  [de-db metadata-db]
  (pprint (run-query de-db (-> (select :id :name) (from :metadata_templates)))))

(defn- run-migration
  [{:keys [host port user de-database metadata-database]}]
  (let [password (get-password host port de-database user)]
    (migrate-metadata-template-tables
     (dbconnect host port de-database user password)
     (dbconnect host port metadata-database user password))))

(defn -main
  [& args]
  (let [{:keys [options arguments summary errors]} (cli/parse-opts args cli-options)]
    (cond
     (seq errors)    (bad-usage summary errors)
     (:help options) (show-help summary)
     :else           (run-migration options))))
