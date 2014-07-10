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
         (not= home-trash (file/dirname path))
         (.uuid irods path))))

(defmethod indexable? CollectionAndDataObjectListingEntry
  [irods entry]
  (indexable? irods (.getFormattedAbsolutePath entry)))


(defn- update-or-remove
  [irods es entity-type entity updater]
  (log/trace "(update-or-remove <irods> <es>" entity-type entity "<updater>)")
  (try
    (updater entity)
    (catch FileNotFoundException _
      (indexing/remove-entity es irods entity-type entity)
      (throw))))


(defn- update-parent-modify-time
  [irods es entity-path]
  (let [parent-path (util/get-parent-path entity-path)]
    (when (and (indexable? irods parent-path) (.exists? irods parent-path))
      (if (indexing/entity-indexed? es irods :collection parent-path)
        (update-or-remove irods
                          es
                          :collection
                          parent-path
                          (partial indexing/update-collection-modify-time es irods))
        (indexing/index-collection es irods parent-path)))))


(defn- rename-entry
  [irods es entity-type old-path new-path index-entity]
  (indexing/remove-entity es irods entity-type old-path)
  (when (indexable? irods new-path) (index-entity new-path))
  (update-parent-modify-time irods es old-path)
  (when-not (= (util/get-parent-path old-path) (util/get-parent-path new-path))
    (update-parent-modify-time irods es new-path)))


; This function is recursive and could blow the stack if a collection tree is deep, like 500 or more
; levels deep.  This is unlikely in iRODS due to the 2700 character path length restriction.
(defn- crawl-collection
  [irods coll-path coll-op obj-op]
  (letfn [(rec-coll-op [coll]
            (coll-op coll)
            (crawl-collection irods (.getFormattedAbsolutePath coll) coll-op obj-op))]
    (doall (map obj-op (.data-objects-in irods coll-path)))
    (doall (map rec-coll-op (.collections-in irods coll-path)))))


(defn- reindex-metadata
  [irods es entity-type path index-entity]
  (if (indexing/entity-indexed? es irods entity-type path)
    (update-or-remove irods
                      es
                      entity-type
                      path
                      (partial indexing/update-metadata es irods entity-type))
    (index-entity path)))


(defn- reindex-collection-metadata
  [irods es path]
  (reindex-metadata irods es :collection path (partial indexing/index-collection es irods)))


(defn- reindex-data-obj-metadata
  [irods es path]
  (reindex-metadata irods es :data-object path (partial indexing/index-data-object es irods)))


(defn- update-acl
  [irods es entity-type entity index-entity]
  (if (indexing/entity-indexed? es irods entity-type entity)
    (update-or-remove irods
                      es
                      entity-type
                      entity
                      (partial indexing/update-acl es irods entity-type))
    (index-entity entity)))


(defn- update-collection-acl
  [irods es coll]
  (when (indexable? irods coll)
    (update-acl irods es :collection coll (partial indexing/index-collection es irods))))


(defn- update-data-object-acl
  [irods es obj]
  (update-acl irods es :data-object obj (partial indexing/index-data-object es irods)))


(defn- index-collection-handler
  [irods es msg]
  (let [collection (:entity msg)]
    (when (indexable? irods collection)
      (indexing/index-collection es irods collection))
    (update-parent-modify-time irods es collection)))


(defn- index-data-object-handler
  [irods es msg]
  (indexing/index-data-object es irods (:entity msg)
    :creator   (:creator msg)
    :file-size (:size msg)
    :file-type (:type msg))
  (update-parent-modify-time irods es (:entity msg)))


(defn- reindex-collection-metadata-handler
  [irods es msg]
  (when (indexable? irods (:entity msg))
    (reindex-collection-metadata irods es (:entity msg))))


(defn- reinidex-coll-dest-metadata-handler
  [irods es msg]
  (when (indexable? irods (:destination msg))
    (reindex-collection-metadata irods es (:destination msg))))


(defn- reindex-data-object-handler
  [irods es msg]
  (let [path (:entity msg)]
    (if (indexing/entity-indexed? es irods :data-object path)
      (update-or-remove irods
                        es
                        :data-object
                        path
                        #(indexing/update-data-object es irods % (:size msg)))
      (indexing/index-data-object es irods path
        :file-size (:size msg)
        :file-type (:type msg)))))


(defn- reindex-data-object-metadata-handler
  [irods es msg]
  (reindex-data-obj-metadata irods es (:entity msg)))


(defn- reinidex-obj-dest-metadata-handler
  [irods es msg]
  (reindex-data-obj-metadata irods es (:destination msg)))


(defn- reindex-multiobject-metadata-handler
  [irods es msg]
  (let [coll-path   (file/dirname (:pattern msg))
        obj-pattern (util/sql-glob->regex (file/basename (:pattern msg)))]
    (doseq [obj (.data-objects-in irods coll-path)]
      (when (re-matches obj-pattern (.getNodeLabelDisplayValue obj))
        (reindex-data-obj-metadata irods es obj)))))


(defn- rename-collection-handler
  [irods es msg]
  (let [old-path (:entity msg)
        new-path (:new-path msg)]
    (rename-entry irods
                  es
                  :collection
                  old-path
                  new-path
                  (partial indexing/index-collection es irods))
    (indexing/remove-entities-like es (file/path-join old-path "*"))
    (crawl-collection irods
                      new-path
                      #(when (indexable? irods %) (indexing/index-collection es irods %))
                      (partial indexing/index-data-object es irods))))


(defn- rename-data-object-handler
  [irods es msg]
  (rename-entry irods
                es
                :data-object
                (:entity msg)
                (:new-path msg)
                (partial indexing/index-data-object es irods)))


(defn- rm-collection-handler
  [irods es msg]
  (indexing/remove-entity es irods :collection (:entity msg))
  (update-parent-modify-time irods es (:entity msg)))


(defn- rm-data-object-handler
  [irods es msg]
  (indexing/remove-entity es irods :data-object (:entity msg))
  (update-parent-modify-time irods es (:entity msg)))


(defn- update-collection-acl-handler
  [irods es msg]
  (when (contains? msg :permission)
    (update-collection-acl irods es (:entity msg))
    (when (:recursive msg)
      (crawl-collection irods
                        (:entity msg)
                        (partial update-collection-acl irods es)
                        (partial update-data-object-acl irods es)))))


(defn- update-data-object-acl-handler
  [irods es msg]
  (update-data-object-acl irods es (:entity msg)))


(defn- update-data-object-sys-meta-handler
  [irods es msg]
  (let [path   (:entity msg)
        update (fn [entity] (indexing/update-data-object es
                                                         irods
                                                         entity
                                                         (.data-object-size irods entity)
                                                         (.data-object-type irods entity)))]
    (if (indexing/entity-indexed? es irods :data-object path)
      (update-or-remove irods es :data-object path update)
      (indexing/index-data-object es irods path))))


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
     es          - elasticsearch connection
     routing-key - The routing key particular to the received message.
     msg         - The change message.

   Throws:
     It throws any exception perculating up from below."
  [irods-cfg es routing-key msg]
  (log/debug "received message:  routing key =" routing-key ", message =" msg)
  (if-let [consume (resolve-consumer routing-key)]
    (try+
      (repo/do-with-irods irods-cfg #(consume % es msg))
      (catch FileNotFoundException _
        (log/info "Attempted to index a non-existent iRODS entity. Most likely it was deleted after"
                  "this index message was created.")))
    (log/warn (str "unknown routing key" routing-key "received with message" msg))))
