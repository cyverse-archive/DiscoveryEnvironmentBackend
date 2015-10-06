(ns facepalm.c192-2014112601
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [me.raynes.fs :as fs]
            [korma.core :as korma]
            [kameleon.sql-reader :as sql]))


(def ^:private version
  "The destination database version."
  "1.9.2:20141126.01")


(defn- exec-sql-statement
  [& statements]
  (let [statement (str/join " " statements)]
    (log/debug "executing SQL statement:" statement)
    (korma/exec-raw statement)))


(defn- load-sql-file
  "Loads a single SQL file into the database."
  [sql-file-path]
  (let [sql-file (fs/file sql-file-path)]
    (println (str "\t\t Loading " sql-file-path "..."))
    (with-open [rdr (io/reader sql-file)]
      (dorun (map exec-sql-statement (sql/sql-statements rdr))))))


(defn convert
  []
  (println "Performing the coversion for" version)
  (load-sql-file "data/20~1_metadata_templates.sql"))
