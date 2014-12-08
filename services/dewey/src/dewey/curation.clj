(ns dewey.curation
  "This namespace contains the logic for handling change messages from iRODS."
  (:use [slingshot.slingshot :only [try+]])
  (:require [clojure.tools.logging :as log]
            [clj-jargon.init :as irods]
            [clojure-commons.file-utils :as file]
            [dewey.entity :as entity]
            [dewey.indexing :as indexing]
            [dewey.util :as util])
  (:import [java.io IOException]
           [java.util UUID]
           [org.irods.jargon.core.exception FileNotFoundException JargonException]))


(defn- extract-entity-id
  [msg]
  (log/trace "extract-entity-id called")
  (UUID/fromString (:entity msg)))


(defn- indexable?
  [entity]
  (log/trace "indexible? called")
  (if-let [path (entity/path entity)]
    (let [parent-path (file/dirname path)
          base        (file/path-join "/" (entity/zone entity))
          home        (file/path-join base "home")
          trash       (file/path-join base "trash")
          home-trash  (file/path-join trash "home")]
      (and (not= home parent-path)
           (not= home-trash parent-path)
           (not= home-trash path)
           (not= trash path)
           (not= home path)
           (not= base path)
           (not= "/" path)))
    false))


(defn- apply-or-remove
  [irods es entity-type entity-id op]
  (log/trace "apply-or-remove <irods> <es>" entity-type entity-id "<op> called")
  (if-let [entity (entity/lookup-entity irods entity-type entity-id)]
    (when (or (= type :data-object) (indexable? entity)) (op entity))
    (indexing/remove-entity es entity-type entity-id)))


; This function is recursive and could blow the stack if a collection tree is deep, like 500 or more
; levels deep.  This is unlikely in iRODS due to the 2700 character path length restriction.
(defn- crawl-collection
  [top-coll coll-op obj-op]
  (log/trace "crawl-collection called")
  (letfn [(rec-coll-op [coll] (log/trace "rec-coll-op called")
                              (coll-op coll)
                              (crawl-collection coll coll-op obj-op))]
    (doall (map obj-op (entity/child-data-objects top-coll)))
    (doall (map rec-coll-op (entity/child-collections top-coll)))))


(defn- update-or-index
  [es entity update index]
  (if (indexing/entity-indexed? es entity)
    (update es entity)
    (index es entity)))


(defn- update-or-index-collection
  [es coll update]
  (update-or-index es coll update indexing/index-collection))


(defn- update-or-index-data-object
  [es obj update]
  (update-or-index es obj update indexing/index-data-object))


; The entity needs to be passed by path, because the entity may not exist in iRODS anymore.
(defn- update-parent-modify-time
  [irods es child-path]
  (log/trace "update-parent-modify-time <irods> <es>" child-path "called")
  (when-let [parent-path (util/get-parent-path child-path)]
    (if-let [parent (entity/lookup-entity irods parent-path)]
      (when (indexable? parent)
        (update-or-index-collection es parent indexing/update-collection-modify-time))
      (do
        (indexing/remove-entities-like es parent-path)
        (recur irods es parent-path)))))


(defn- reindex-collection-metadata
  [es coll]
  (update-or-index-collection es coll indexing/update-metadata))


(defn- reindex-data-obj-metadata
  [es obj]
  (update-or-index-collection es obj indexing/update-metadata))


(defn- update-collection-acl
  [es coll]
  (update-or-index-collection es coll indexing/update-acl))


(defn- update-data-object-acl
  [es obj]
  (update-or-index-data-object es obj indexing/update-acl))


(defn- update-collection-path
  [irods es coll new-path]
  (log/trace "update-collection-path called")
  (letfn [(update-child-coll-path [_ child]
            (log/trace "update-child-coll-path")
            (indexing/update-path es child (entity/path child) (entity/modification-time child)))

          (update-child-obj-path [_ child]
            (log/trace "update-child-obj-path")
            (indexing/update-path es child (entity/path child)))

          (update-child-coll [child]
            (log/trace "update-child-coll called")
            (when (indexable? child) (update-or-index-collection es child update-child-coll-path)))

          (update-child-obj [child]
            (log/trace "update-child-obj called")
            (update-or-index-collection es child update-child-obj-path))]
    (update-or-index-collection es coll #(indexing/update-path %1 %2 new-path))
    (update-parent-modify-time irods es new-path)
    (crawl-collection coll update-child-coll update-child-obj)))


(defn- index-collection-handler
  [irods es msg]
  (log/trace "index-collection-handler called")
  (let [index (partial indexing/index-collection es)
        id    (extract-entity-id msg)]
    (apply-or-remove irods es :collection id index)
    (update-parent-modify-time irods es (:path msg))))


(defn- index-data-object-handler
  [irods es msg]
  (let [index (fn [obj] (indexing/index-data-object es obj
                          :creator   (:creator msg)
                          :file-size (:size msg)
                          :file-type (:type msg)))
        id    (extract-entity-id msg)]
    (apply-or-remove irods es :data-object id index)
    (update-parent-modify-time irods es (:path msg))))


(defn- reindex-collection-metadata-handler
  [irods es msg]
  (let [reindex (partial reindex-collection-metadata es)
        id      (extract-entity-id msg)]
    (apply-or-remove irods es :collection id reindex)))


(defn- reinidex-coll-dest-metadata-handler
  [irods es msg]
  (let [reindex (partial reindex-collection-metadata es)
        dest-id (UUID/fromString (:destination msg))]
    (apply-or-remove irods es :collection dest-id reindex)))


(defn- reindex-data-object-handler
  [irods es msg]
  (let [reindex (fn [obj] (update-or-index es
                                           obj
                                           #(indexing/update-data-object %1 %2 (:size msg))
                                           #(indexing/index-data-object %1 %2
                                              :file-size (:size msg)
                                              :file-type (:type msg))))
        id      (extract-entity-id msg)]
    (apply-or-remove irods es :data-object id reindex)))


(defn- reindex-data-object-metadata-handler
  [irods es msg]
  (let [reindex (partial reindex-data-obj-metadata es)
        id      (extract-entity-id msg)]
    (apply-or-remove irods es :data-object id reindex)))


(defn- reinidex-obj-dest-metadata-handler
  [irods es msg]
  (let [reindex (partial reindex-data-obj-metadata es)
        dest-id (UUID/fromString (:destination msg))]
    (apply-or-remove irods es :data-object dest-id reindex)))


(defn- reindex-multiobject-metadata-handler
  [irods es msg]
  (let [coll-path   (file/dirname (:pattern msg))
        obj-pattern (util/sql-glob->regex (file/basename (:pattern msg)))]
    (if-let [coll (entity/lookup-entity irods coll-path)]
      (doseq [obj (entity/child-data-objects-like coll obj-pattern)]
        (reindex-data-obj-metadata es obj))
      (do
        (indexing/remove-entities-like es coll-path)
        (update-parent-modify-time irods es coll-path)))))


(defn- rm-collection-handler
  [irods es msg]
  (indexing/remove-entity es :collection (extract-entity-id msg))
  (update-parent-modify-time irods es (:path msg)))


(defn- rm-data-object-handler
  [irods es msg]
  (indexing/remove-entity es :data-object (extract-entity-id msg))
  (update-parent-modify-time irods es (:path msg)))


(defn- update-collection-acl-handler
  [irods es msg]
  (log/trace "update-collection-acl-handler called")
  (when (contains? msg :permission)
    (let [update (fn [coll] (update-collection-acl es coll)
                            (when (:recursive msg)
                              (crawl-collection coll
                                                (partial update-collection-acl es)
                                                (partial update-data-object-acl es))))
          id     (extract-entity-id msg)]
      (apply-or-remove irods es :collection id update))))


(defn- update-collection-path-handler
  [irods es msg]
  (log/trace "update-collection-path-handler called")
  (let [update (fn [coll] (update-collection-path irods es coll (:new-path msg)))
        id     (extract-entity-id msg)]
    (apply-or-remove irods es :collection id update)
    (update-parent-modify-time irods es (:old-path msg))))


(defn- update-data-object-acl-handler
  [irods es msg]
  (let [update (partial update-data-object-acl es)
        id     (extract-entity-id msg)]
    (apply-or-remove irods es :data-object id update)))


(defn- update-data-object-path-handler
  [irods es msg]
  (log/trace "update-data-object-path-handler called")
  (let [update (fn [obj]
                 (update-or-index-data-object es obj #(indexing/update-path %1 %2 (:new-path msg)))
                 (update-parent-modify-time irods es (:new-path msg)))
        id     (extract-entity-id msg)]
    (apply-or-remove irods es :data-object id update)
    (update-parent-modify-time irods es (:old-path msg))))


(defn- update-data-object-sys-meta-handler
  [irods es msg]
  (let [update (fn [_ obj]
                 (indexing/update-data-object es obj (entity/size obj) (entity/media-type obj)))
        apply  (fn [obj]
                 (update-or-index-data-object es obj update)
                 (indexing/index-data-object es obj))
        id     (extract-entity-id msg)]
    (apply-or-remove irods es :data-object id apply)))


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
    "collection.mv"                update-collection-path-handler
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
    "data-object.mv"               update-data-object-path-handler
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
      (irods/with-jargon irods-cfg [irods]
        (consume irods es msg))
      (catch FileNotFoundException _
        (log/info "Attempted to index a non-existent iRODS entity. Most likely it was deleted after"
                  "this index message was created."))
      (catch JargonException e
        (if (instance? IOException (.getCause e))
          (log/warn "Failed to connect to iRODs. Could not process route" routing-key "with message"
            msg)
          (throw e))))
    (log/warn (str "unknown routing key" routing-key "received with message" msg))))
