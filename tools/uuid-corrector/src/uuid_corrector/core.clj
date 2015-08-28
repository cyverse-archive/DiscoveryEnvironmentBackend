(ns uuid-corrector.core
  (:gen-class)
  (:require [clojure.pprint :refer [pprint]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
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

(defn- build-spec
  [host port db user password]
  {:subprotocol "postgresql"
   :subname     (str "//" host ":" port "/" db)
   :user        user
   :password    password})

(def uuid-columns [:created_by :modified_by])
(def tables [:templates :attributes])
(def table-columns (mapcat (fn [table] (map #(list table %) uuid-columns)) tables))

(defn- update-uuids
  [table column uuid username]
  (-> (update table)
      (sset {column username})
      (where [:= (sql/call :cast column :varchar) (sql/call :cast uuid :varchar)])))

(defn- get-usernames
  [uuids]
  (-> (select :id (sql/call :regexp_replace :username "@iplantcollaborative.org$" ""))
      (from :users)
      (where [:in (sql/call :cast :id :varchar) uuids])))

(defn- get-uuids
  []
  {:union (map (fn [[table column]] (-> (select column) (from table))) table-columns)})

(defn- migrate-table
  [table meta-trans uuid-mappings]
  (println (str "Migrating " table))
  (doseq [column uuid-columns
          [uuid username] uuid-mappings]
     (jdbc/execute! meta-trans (sql/format (update-uuids table column uuid username)))))

(defn- migrate-uuids
  [de-db metadata-db]
  (try
    (jdbc/with-db-transaction [meta-trans metadata-db]
      (jdbc/with-db-transaction [de-trans de-db]
        (let [uuids (drop 1 (jdbc/query meta-trans (sql/format (get-uuids)) :as-arrays? true :row-fn first))
              uuid-mappings (drop 1 (jdbc/query de-trans (sql/format (get-usernames uuids)) :as-arrays? true))]
         (dorun (map #(migrate-table % meta-trans uuid-mappings) tables)))))
    (catch java.sql.SQLException e
      (log/error e)
      (when (.getNextException e)
        (log/error (.getNextException e)))
      (throw e))))

(defn- run-migration
  [{:keys [host port user de-database metadata-database]}]
  (let [password (get-password host port de-database user)
        de-spec  (build-spec host port de-database user password)
        md-spec  (build-spec host port metadata-database user password)]
    (with-open [de-conn (jdbc/get-connection de-spec)
                md-conn (jdbc/get-connection md-spec)]
      (migrate-uuids
       (jdbc/add-connection de-spec de-conn)
       (jdbc/add-connection md-spec md-conn)))))

(defn -main
  [& args]
  (let [{:keys [options arguments summary errors]} (cli/parse-opts args cli-options)]
    (cond
     (seq errors)    (bad-usage summary errors)
     (:help options) (show-help summary)
     :else           (run-migration options))))
