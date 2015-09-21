(ns data-info.services.uuids
  (:use [clj-jargon.permissions]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clj-icat-direct.icat :as icat]
            [data-info.services.stat :as stat]
            [clj-jargon.by-uuid :as uuid]
            [clj-jargon.init :as init]
            [clojure-commons.error-codes :as error]
            [data-info.util.config :as cfg]
            [data-info.util.irods :as irods]
            [data-info.util.validators :as valid])
  (:import [java.util UUID]
           [clojure.lang IPersistentMap]))


(defn ^IPersistentMap path-for-uuid
  "Resolves a stat info for the entity with a given UUID.

   Params:
     user - the user requesting the info
     uuid - the UUID

   Returns:
     It returns a path-stat map containing an additional UUID field."
  ([^IPersistentMap cm ^String user ^UUID uuid]
    (if-let [path (uuid/get-path cm uuid)]
      (assoc (stat/path-stat cm user path) :uuid uuid)
      (throw+ {:error_code error/ERR_DOES_NOT_EXIST :uuid uuid})))

  ([^String user ^UUID uuid]
   (init/with-jargon (cfg/jargon-cfg) [cm]
     (path-for-uuid cm user uuid))))


(defn paths-for-uuids
  [user uuids]
  (letfn [(id-type [type entity] (merge entity {:id (:path entity) :type type}))]
    (init/with-jargon (cfg/jargon-cfg) [cm]
      (valid/user-exists cm user)
      (->> (concat (map (partial id-type :dir) (icat/select-folders-with-uuids uuids))
                   (map (partial id-type :file) (icat/select-files-with-uuids uuids)))
        (mapv (partial stat/decorate-stat cm user))
        (remove #(nil? (:permission %)))))))

(defn ^IPersistentMap uuid-for-path
  "Retrieves the path stat info for a given entity. It attaches the UUID in a additional :uuid
   field.

   Parameters:
     cm   - the open jargon context map
     user - the user making the request
     path - the absolute path to the entity

   Returns:
     It returns the modified path stat map."
  [^IPersistentMap cm ^String user ^String path]
  (assoc (stat/path-stat cm user path) :uuid (irods/lookup-uuid cm path)))


(defn uuids-for-paths
  [user paths]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (valid/user-exists cm user)
    (valid/all-paths-exist cm paths)
    (valid/all-paths-readable cm user paths)
    (remove nil? (mapv (partial uuid-for-path cm user) paths))))

(defn ^Boolean uuid-accessible?
  "Indicates if a data item is readable by a given user.

   Parameters:
     user     - the authenticated name of the user
     data-id  - the UUID of the data item

   Returns:
     It returns true if the user can access the data item, otherwise false"
  [^String user ^UUID data-id]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (let [data-path (:path (path-for-uuid cm user (str data-id)))]
      (and data-path (is-readable? cm user data-path)))))
