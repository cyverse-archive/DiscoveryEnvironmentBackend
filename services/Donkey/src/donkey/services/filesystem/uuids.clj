(ns donkey.services.filesystem.uuids
  (:use [clj-jargon.init]
        [clj-jargon.metadata]
        [clj-jargon.permissions]
        [clojure-commons.validators]
        [donkey.util.config]
        [clojure-commons.error-codes]
        [slingshot.slingshot :only [try+ throw+]]
        [donkey.services.filesystem.validators])
  (:require [clojure.tools.logging :as log]
            [clj-icat-direct.icat :as icat]
            [donkey.services.filesystem.stat :as stat]
            [cheshire.core :as json]))

(def uuid-attr "ipc_UUID")

(defn path-for-uuid
  "Returns a map of the form
     {:uuid <uuid>
      :path \"/path/to/file/or/folder\"}
  for the UUID passed in. Looks in the ipc_UUID AVU for the UUID."
  [cm user uuid]
  (let [folders (list-collections-with-attr-value cm uuid-attr uuid)
        files   (list-files-with-avu cm uuid-attr := uuid)
        results (concat folders files)]
    (when (empty? results)
      (throw+ {:error_code ERR_DOES_NOT_EXIST
               :uuid uuid}))
    (when (> (count results) 1)
      (log/warn "Too many results for" uuid ":" (count results))
      (log/debug "Results for" uuid ":" results)
      (throw+ {:error_code ERR_TOO_MANY_RESULTS
               :count (count results)
               :uuid uuid}))
    (if (pos? (count results))
      (merge {:uuid uuid} (stat/path-stat cm user (first results))))))

(defn paths-for-uuids
  [user uuids]
  (with-jargon (jargon-cfg) [cm]
    (user-exists cm user)
    (->> (concat (icat/select-folders-with-uuids uuids)
                 (icat/select-files-with-uuids uuids))
      (mapv #(merge % (stat/path-stat cm user (:path %))))
      (filter #(and (not (nil? %))
                    (is-readable? cm user (:id %)))))))

(defn do-paths-for-uuids
  [params body]
  (validate-map params {:user string?})
  (validate-map body {:uuids sequential?})
  (json/encode {:paths (paths-for-uuids (:user params) (:uuids body))}))

(defn uuid-for-path
  [cm user path]
  (let [attrs (get-attribute cm path uuid-attr)]
    (when-not (pos? (count attrs))
      (log/warn "Missing UUID for" path)
      (throw+ {:error_code ERR_NOT_FOUND
               :path path}))
    (if (pos? (count attrs))
      (merge {:uuid (:value (first attrs))}
             (stat/path-stat cm user path)))))

(defn uuids-for-paths
  [user paths]
  (with-jargon (jargon-cfg) [cm]
    (user-exists cm user)
    (all-paths-exist cm paths)
    (all-paths-readable cm user paths)
    (filter #(not (nil? %)) (mapv (partial uuid-for-path cm user) paths))))

(defn do-uuids-for-paths
  [params body]
  (log/warn body)
  (validate-map params {:user string?})
  (validate-map body {:paths sequential?})
  (json/encode {:paths (uuids-for-paths (:user params) (:paths body))}))

