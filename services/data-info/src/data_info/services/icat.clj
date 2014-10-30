(ns data-info.services.icat
  (:use [clj-icat-direct.icat :only [icat-db-spec setup-icat]])
  (:require [data-info.util.config :as cfg]
            [clojure.tools.logging :as log]
            [clj-jargon.init :as init]
            [clj-jargon.metadata :as meta])
  (:import [clojure.lang IPersistentMap]
           [java.util UUID]))


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
  (log/info "[ICAT] set up ICAT connection.")
  (setup-icat (spec)))


(defn ^String resolve-data-type
  "Given filesystem id, it returns the type of the entry it is, file or folder.

   Parameters:
     fs       - (optional) An open jargon context
     entry-id - The UUID of the entry to inspect

   Returns:
     The type of the entry, `file` or `folder`"
  ([^IPersistentMap fs ^UUID entry-id]
   (if (empty? (meta/list-collections-with-attr-value fs "ipc_UUID" entry-id)) "file" "folder"))

  ([^UUID entry-id]
   (init/with-jargon (cfg/jargon-cfg) [fs]
     (resolve-data-type fs entry-id))))