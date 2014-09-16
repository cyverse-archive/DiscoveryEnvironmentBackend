(ns donkey.util.icat
  (:use [clj-icat-direct.icat :only [icat-db-spec setup-icat]])
  (:require [donkey.util.config :as cfg]
            [clojure.tools.logging :as log]
            [clj-jargon.metadata :as meta]))

(defn- spec
  []
  (icat-db-spec
    (cfg/icat-host)
    (cfg/icat-user)
    (cfg/icat-password)
    :port (cfg/icat-port)
    :db   (cfg/icat-db)))

(defn configure-icat
  "Configures the connection pool to the ICAT database."
  []
  (log/warn "[ICAT] set up ICAT connection.")
  (setup-icat (spec)))

(defn resolve-data-type
  "Given filesystem id, it returns the type of the entry it is, file or folder.

   Parameters:
     fs - An open jargon context
     entry-id - The UUID of the entry to inspect

   Returns:
     The type of the entry, file or folder"
  [fs entry-id]
  (if (empty? (meta/list-collections-with-attr-value fs "ipc_UUID" entry-id))
    "file"
    "folder"))