(ns clockwork.tree-urls
  (:use [clj-jargon.init :only [with-jargon]]
        [clj-time.core :only [after? days now plus]]
        [clockwork.config
         :only [jargon-config tree-urls-bucket tree-urls-avu tree-urls-cleanup-age]]
        [slingshot.slingshot :only [try+]])
  (:require [clj-jargon.metadata :as metajargon]
            [clockwork.riak :as riak]
            [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]))

(defn- get-tree-url-keys
  "Gets the list of keys in the tree URLs bucket."
  []
  (try+
   (doto (riak/list-keys (tree-urls-bucket))
     (#(log/debug "tree URL keys:" %)))
   (catch [:error_code ce/ERR_REQUEST_FAILED] {:keys [body]}
     (log/error "unable to get the list of tree URL keys -" body)
     [])
   (catch Exception e
     (log/error e "unable to get the list of tree URL keys")
     [])))

(defn- associated-with-file?
  "Determines if a tree URL key is associated with any file in iRODS."
  [cm k]
  (let [path (:path (riak/object-url (tree-urls-bucket) k))]
    (doto (pos? (count (metajargon/list-files-with-avu cm (tree-urls-avu) := path)))
      (#(log/debug "key" k (if % "is" "is not") "associated with a file in iRODS")))))

(defn- remove-referenced-keys
  "Removes any tree URL keys that are referenced in iRODS from a collection."
  [ks]
  (with-jargon (jargon-config) [cm]
    (doall (remove (partial associated-with-file? cm) ks))))

(defn too-young-to-delete?
  "Determines if a tree URL key was last modified too recently for it to be deleted."
  [min-age k]
  (let [too-young? #(after? (plus % (days min-age)) (now))
        mod-time   (riak/object-last-modified (tree-urls-bucket) k)]
    (cond
     (nil? mod-time)       (do (log/debug "key" k "does not exist") true)
     (too-young? mod-time) (do (log/debug "key" k "is too young to delete") true)
     :else                 (do (log/debug "key" k "may be deleted") false))))

(defn- remove-recent-tree-urls
  "Removes any tree URL keys that are younger than the minimum cleanup age from a collection."
  [ks]
  (doall (remove (partial too-young-to-delete? (tree-urls-cleanup-age)) ks)))

(defn- delete-object
  "Deletes a tree URL object."
  [k]
  (try+
   (log/info "deleting tree URLs for key" k)
   (riak/remove-object (tree-urls-bucket) k)
   (catch [:error_code ce/ERR_REQUEST_FAILED] {:keys [body]}
     (log/warn "unable to delete tree URL object" k "-" body))
   (catch Exception e
     (log/warn e "unable to delete tree URL object" k))))

(defn clean-up-old-tree-urls
  "Removes tree URL objects that are no longer referenced in iRODS and are old enough to be cleaned
   up.  This is the function that implements the clean-up-old-tree-urls job."
  []
  (log/info "removing unreferenced tree URLs from external storage")
  (try
    (dorun
     (->> (get-tree-url-keys)
          (remove-referenced-keys)
          (remove-recent-tree-urls)
          (map delete-object)))
    (catch Exception e
      (log/error e "unexpected error in remove-old-trees")))
  (log/info "unreferenced tree URL removal completed"))
