(ns data-info.util.irods
  "This namespace encapsulates all of the common iRODS access logic."
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+]]
            [clj-jargon.by-uuid :as uuid]
            [clj-jargon.init :as init]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.metadata :as meta]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as file]
            [data-info.util.config :as cfg])
  (:import [clojure.lang IPersistentMap]
           [java.util UUID]
           [org.apache.tika Tika]))


(defn ^String abs-path
  "Resolves a path relative to a zone into its absolute path.

   Parameters:
     zone         - the name of the zone
     path-in-zone - the path relative to the zone

   Returns:
     It returns the absolute path."
  [^String zone ^String path-in-zone]
  (file/path-join "/" zone path-in-zone))


(defn ^UUID lookup-uuid
  "Retrieves the UUID associated with a given entity path.

   Parameters:
     cm   - the jargon context map
     path - the path to the entity

   Returns:
     It returns the UUID."
  [^IPersistentMap cm ^String path]
  (let [attrs (meta/get-attribute cm path uuid/uuid-attr)]
    (when-not (pos? (count attrs))
      (log/warn "Missing UUID for" path)
      (throw+ {:error_code error/ERR_NOT_FOUND :path path}))
    (-> attrs first :value UUID/fromString)))


(defn ^String detect-media-type
  "detects the media type of a given file

   Parameters:
     cm   - (OPTIONAL) an open jargon context
     path - the absolute path to the file

   Returns:
     It returns the media type."
  ([^IPersistentMap cm ^String path]
   (let [path-type (.detect (Tika.) (file/basename path))]
     (if (or (= path-type "application/octet-stream")
             (= path-type "text/plain"))
       (.detect (Tika.) (ops/input-stream cm path))
       path-type)))

  ([^String path]
   (init/with-jargon (cfg/jargon-cfg) [cm]
     (detect-media-type cm path))))
