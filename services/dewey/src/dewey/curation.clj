(ns dewey.curation
  "This namespace contains the logic for handling change messages from iRODS."
  (:use [slingshot.slingshot :only [try+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as file]
            [dewey.indexing :as indexing]
            [dewey.repo :as repo]
            [dewey.util :as util])
  (:import [org.irods.jargon.core.exception FileNotFoundException]
           [org.irods.jargon.core.query CollectionAndDataObjectListingEntry]))


(defmulti ^{:private true} indexable? #(type %2))

(defmethod indexable? String
  [irods path]
  (let [base       (file/path-join "/" (.zone irods))
        home       (file/path-join base "home")
        trash      (file/path-join base "trash")
        home-trash (file/path-join trash "home")]
    (and (not= base path)
         (not= home path)
         (not= trash path)
         (not= home-trash path)
         (not= home (file/dirname path))
         (not= home-trash (file/dirname path)))))

(defmethod indexable? CollectionAndDataObjectListingEntry
  [irods entry]
  (indexable? irods (.getFormattedAbsolutePath entry)))


(defn- update-or-remove
  [entity-type entity updater]
  (log/trace "(update-or-remove" entity-type entity "<updater>)")
  (try
    (updater entity)
    (catch FileNotFoundException _
      (indexing/remove-entity entity-type entity)
      (throw))))


(defn- update-parent-modify-time
  [irods entity-path]
  (let [parent-path (util/get-parent-path entity-path)]
    (when (and (indexable? irods parent-path) (.exists? irods parent-path))
      (if (indexing/entity-indexed? :collection parent-path)
        (update-or-remove :collection
                          parent-path
                          (partial indexing/update-collection-modify-time irods))
        (indexing/index-collection irods parent-path)))))


(defn- rename-entry
  [irods entity-type old-path new-path index-entity]
  (indexing/remove-entity entity-type old-path)
  (when (indexable? irods new-path)
    (index-entity irods new-path))
  (update-parent-modify-time irods old-path)
  (when-not (= (util/get-parent-path old-path) (util/get-parent-path new-path))
    (update-parent-modify-time irods new-path)))


; This function is recursive and could blow the stack if a collection tree is deep, like 500 or more
; levels deep.  This is unlikely in iRODS due to the 2700 character path length restriction.
(defn- crawl-collection
  [irods coll-path coll-op obj-op]
  (letfn [(rec-coll-op [coll]
            (coll-op irods coll)
            (crawl-collection irods (.getFormattedAbsolutePath coll) coll-op obj-op))]
    (doall (map (partial obj-op irods) (.data-objects-in irods coll-path)))
    (doall (map rec-coll-op (.collections-in irods coll-path)))))


(defn- reindex-metadata
  [irods entity-type path index-entity]
  (if (indexing/entity-indexed? entity-type path)
    (update-or-remove entity-type path (partial indexing/update-metadata irods entity-type))
    (index-entity irods path)))


(defn- reindex-collection-metadata
  [irods path]
  (reindex-metadata irods :collection path indexing/index-collection))


(defn- reindex-data-obj-metadata
  [irods path]
  (reindex-metadata irods :data-object path indexing/index-data-object))


(defn- update-acl
  [irods entity-type entity index-entity]
  (if (indexing/entity-indexed? entity-type entity)
    (update-or-remove entity-type entity (partial indexing/update-acl irods entity-type))
    (index-entity irods entity)))


(defn- update-collection-acl
  [irods coll]
  (when (indexable? irods coll)
    (update-acl irods :collection coll indexing/index-collection)))


(defn- update-data-object-acl
  [irods obj]
  (update-acl irods :data-object obj indexing/index-data-object))


(defn- index-collection-handler
  [irods msg]
  (let [collection (:entity msg)]
    (when (indexable? irods collection)
      (indexing/index-collection irods collection))
    (update-parent-modify-time irods collection)))


(defn- index-data-object-handler
  [irods msg]
  (indexing/index-data-object irods (:entity msg)
    :creator   (:creator msg)
    :file-size (:size msg)
    :file-type (:type msg))
  (update-parent-modify-time irods (:entity msg)))


(defn- reindex-collection-metadata-handler
  [irods msg]
  (when (indexable? irods (:entity msg))
    (reindex-collection-metadata irods (:entity msg))))


(defn- reinidex-coll-dest-metadata-handler
  [irods msg]
  (when (indexable? irods (:destination msg))
    (reindex-collection-metadata irods (:destination msg))))


(defn- reindex-data-object-handler
  [irods msg]
  (let [path (:entity msg)]
    (if (indexing/entity-indexed? :data-object path)
      (update-or-remove :data-object path #(indexing/update-data-object irods % (:size msg)))
      (indexing/index-data-object irods path
        :file-size (:size msg)
        :file-type (:type msg)))))


(defn- reindex-data-object-metadata-handler
  [irods msg]
  (reindex-data-obj-metadata irods (:entity msg)))


(defn- reinidex-obj-dest-metadata-handler
  [irods msg]
  (reindex-data-obj-metadata irods (:destination msg)))


(defn- reindex-multiobject-metadata-handler
  [irods msg]
  (let [coll-path   (file/dirname (:pattern msg))
        obj-pattern (util/sql-glob->regex (file/basename (:pattern msg)))]
    (doseq [obj (.data-objects-in irods coll-path)]
      (when (re-matches obj-pattern (.getNodeLabelDisplayValue obj))
        (reindex-data-obj-metadata irods obj)))))


(defn- rename-collection-handler
  [irods msg]
  (let [old-path (:entity msg)
        new-path (:new-path msg)]
    (rename-entry irods :collection old-path new-path indexing/index-collection)
    (indexing/remove-entities-like (file/path-join old-path "*"))
    (crawl-collection irods
                      new-path
                      #(when (indexable? %1 %2) (indexing/index-collection %1 %2))
                      indexing/index-data-object)))


(defn- rename-data-object-handler
  [irods msg]
  (rename-entry irods :data-object (:entity msg) (:new-path msg) indexing/index-data-object))


(defn- rm-collection-handler
  [irods msg]
  (indexing/remove-entity :collection (:entity msg))
  (update-parent-modify-time irods (:entity msg)))


(defn- rm-data-object-handler
  [irods msg]
  (indexing/remove-entity :data-object (:entity msg))
  (update-parent-modify-time irods (:entity msg)))


(defn- update-collection-acl-handler
  [irods msg]
  (when (contains? msg :permission)
    (update-collection-acl irods (:entity msg))
    (when (:recursive msg)
      (crawl-collection irods (:entity msg) update-collection-acl update-data-object-acl))))


(defn- update-data-object-acl-handler
  [irods msg]
  (update-data-object-acl irods (:entity msg)))


(defn- update-data-object-sys-meta-handler
  [irods msg]
  (let [path   (:entity msg)
        update (fn [entity] (indexing/update-data-object irods
                                                         entity
                                                         (.data-object-size irods entity)
                                                         (.data-object-type irods entity)))]
    (if (indexing/entity-indexed? :data-object path)
      (update-or-remove :data-object path update)
      (indexing/index-data-object irods path))))


(defn- resolve-consumer
  [routing-key]
  (case routing-key
    "collection.acl.mod"           update-collection-acl-handler
    "collection.add"               index-collection-handler
    "collection.metadata.add"      reindex-collection-metadata-handler
    "collection.metadata.adda"     reindex-collection-metadata-handler
    "collection.metadata.cp"       reinidex-coll-dest-metadata-handler
    "collection.metadata.mod"      reindex-collection-metadata-handler
    "collection.metadata.rm"       reindex-collection-metadata-handler
    "collection.metadata.rmw"      reindex-collection-metadata-handler
    "collection.metadata.set"      reindex-collection-metadata-handler
    "collection.mv"                rename-collection-handler
    "collection.rm"                rm-collection-handler
    "data-object.acl.mod"          update-data-object-acl-handler
    "data-object.add"              index-data-object-handler
    "data-object.cp"               index-data-object-handler
    "data-object.metadata.add"     reindex-data-object-metadata-handler
    "data-object.metadata.adda"    reindex-data-object-metadata-handler
    "data-object.metadata.addw"    reindex-multiobject-metadata-handler
    "data-object.metadata.cp"      reinidex-obj-dest-metadata-handler
    "data-object.metadata.mod"     reindex-data-object-metadata-handler
    "data-object.metadata.rm"      reindex-data-object-metadata-handler
    "data-object.metadata.rmw"     reindex-data-object-metadata-handler
    "data-object.metadata.set"     reindex-data-object-metadata-handler
    "data-object.mod"              reindex-data-object-handler
    "data-object.mv"               rename-data-object-handler
    "data-object.rm"               rm-data-object-handler
    "data-object.sys-metadata.mod" update-data-object-sys-meta-handler
                                   nil))


(defn consume-msg
  "This is the primary function. It dispatches the message based on a routing key to a function
   specific to a certain type of message.

   Parameters:
     irods-cfg   - An irods configuration map for an initialized clj-jargon library.
     routing-key - The routing key particular to the received message.
     msg         - The change message.

   Throws:
     It throws any exception perculating up from below."
  [irods-cfg routing-key msg]
  (log/debug "received message:  routing key =" routing-key ", message =" msg)
  (if-let [consume (resolve-consumer routing-key)]
    (try+
      (repo/do-with-irods irods-cfg #(consume % msg))
      (catch FileNotFoundException _
        (log/info "Attempted to index and non-existent iRODS entity. Most likely it was deleted"
                  "after this index message was created.")))
    (log/warn (str "unknown routing key" routing-key "received with message" msg))))
