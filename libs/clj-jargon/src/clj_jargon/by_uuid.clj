(ns clj-jargon.by-uuid
  (:require [clojure.tools.logging :as log]
           [slingshot.slingshot :refer [throw+]]
           [clojure-commons.error-codes :as error]
           [clj-jargon.metadata :as meta])
  (:import [clojure.lang IPersistentMap]
           [java.util UUID]))


(def ^String uuid-attr
  "This is the iRODS metadata attribute that holds the UUID."
  "ipc_UUID")


(defn ^String get-path
  "Returns the path of an entity given its UUID.

   Parameters:
     cm   - an open jargon context
     uuid - the UUID of the entity

   Returns:
     If found, it returns the path of the entity."
  [^IPersistentMap cm ^UUID uuid]
  (let [results (meta/list-everything-with-attr-value cm uuid-attr uuid)]
    (when-not (empty? results)
      (when (> (count results) 1)
        (log/error "Too many results for" uuid ":" (count results))
        (log/debug "Results for" uuid ":" results)
        (throw+ {:error_code error/ERR_TOO_MANY_RESULTS
                 :count      (count results)
                 :uuid       uuid}))
      (first results))))
