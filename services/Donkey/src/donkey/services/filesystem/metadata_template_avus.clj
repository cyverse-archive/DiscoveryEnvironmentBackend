(ns donkey.services.filesystem.metadata-template-avus
  (:use [clj-jargon.init :only [with-jargon]]
        [donkey.services.filesystem.common-paths]
        [kameleon.uuids :only [uuidify]])
  (:require [clojure-commons.validators :as common-validators]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.clients.metadata.raw :as metadata]
            [donkey.services.filesystem.icat :as icat]
            [donkey.services.filesystem.uuids :as uuids]
            [donkey.services.filesystem.validators :as validators]))

(defn- get-metadata-copy-src-path
  [cm user src-id]
  (let [src-path (:path (uuids/path-for-uuid cm user src-id))]
    (validators/path-readable cm user src-path)
    src-path))

(defn- get-metadata-copy-dest-paths
  [cm user dest-ids]
  (let [dest-items (map (partial uuids/path-for-uuid cm user) dest-ids)
        dest-paths (map :path dest-items)]
    (validators/all-paths-writeable cm user dest-paths)
    dest-items))

(defn- format-copy-dest-item
  [{:keys [id type]}]
  {:id   id
   :type (metadata/resolve-data-type type)})

(defn copy-metadata-template-avus
  "Copies all AVUs from the data item with data-id to dest-ids with the metadata service, and
   returns a map of the source data item's path and the destination data items' paths."
  ([user force? data-id dest-ids]
    (with-jargon (icat/jargon-cfg) [cm]
      (copy-metadata-template-avus cm user force? data-id dest-ids)))
  ([cm user force? data-id dest-ids]
    (validators/user-exists cm user)
    (let [src-path (get-metadata-copy-src-path cm user data-id)
          dest-items (get-metadata-copy-dest-paths cm user dest-ids)]
      (metadata/copy-metadata-template-avus data-id force? (map format-copy-dest-item dest-items))
      {:user  user
       :src   src-path
       :paths (map :path dest-items)})))

(defn do-metadata-template-avu-list
  "Lists AVUs associated with a Metadata Template for the given user's data item."
  ([params data-id]
   (metadata/list-metadata-avus (uuidify data-id)))

  ([params data-id template-id]
   (metadata/list-metadata-template-avus (uuidify data-id) (uuidify template-id))))

(with-pre-hook! #'do-metadata-template-avu-list
  (fn [params data-id & [template-id]]
    (log-call "do-metadata-template-avu-list" params data-id template-id)
    (with-jargon (icat/jargon-cfg) [cm]
      (common-validators/validate-map params {:user string?})
      (validators/user-exists cm (:user params))
      (let [user (:user params)
            path (:path (uuids/path-for-uuid cm user data-id))]
        (validators/path-readable cm user path)))))

(with-post-hook! #'do-metadata-template-avu-list (log-func "do-metadata-template-avu-list"))

(defn do-set-metadata-template-avus
  "Adds or Updates AVUs associated with a Metadata Template for the given user's data item."
  [{username :user} data-id template-id body]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm username)
    (let [data-id (uuidify data-id)
          template-id (uuidify template-id)
          {:keys [path type]} (uuids/path-for-uuid cm username data-id)
          data-type (metadata/resolve-data-type type)]
      (validators/path-writeable cm username path)
      (metadata/set-metadata-template-avus data-id data-type template-id body))))

(with-pre-hook! #'do-set-metadata-template-avus
  (fn [params data-id template-id body]
    (log-call "do-set-metadata-template-avus" params data-id template-id body)
    (common-validators/validate-map params {:user string?})))

(with-post-hook! #'do-set-metadata-template-avus (log-func "do-set-metadata-template-avus"))

(defn do-remove-metadata-template-avus
  "Removes AVUs associated with a Metadata Template for the given user's data item."
  ([params data-id template-id]
   (metadata/remove-metadata-template-avus (uuidify data-id) (uuidify template-id)))

  ([params data-id template-id avu-id]
   (metadata/remove-metadata-template-avu (uuidify data-id) (uuidify template-id) (uuidify avu-id))))

(with-pre-hook! #'do-remove-metadata-template-avus
  (fn [{user :user :as params} data-id template-id & [avu-id]]
    (log-call "do-remove-metadata-template-avus" params data-id template-id avu-id)
    (common-validators/validate-map params {:user string?})
    (with-jargon (icat/jargon-cfg) [cm]
      (validators/user-exists cm user)
      (let [path (:path (uuids/path-for-uuid cm user data-id))]
        (validators/path-writeable cm user path)))))

(with-post-hook! #'do-remove-metadata-template-avus (log-func "do-remove-metadata-template-avus"))

(defn do-copy-metadata-template-avus
  "Copies Metadata Template AVUs from the given user's data item to other data items."
  [{:keys [user]} data-id force {dest-ids :destination_ids}]
  (copy-metadata-template-avus user (Boolean/parseBoolean force) (uuidify data-id) (map uuidify dest-ids)))

(with-pre-hook! #'do-copy-metadata-template-avus
  (fn [{:keys [user] :as params} data-id force {dest-ids :destination_ids :as body}]
    (log-call "do-copy-metadata-template-avus" params data-id body)
    (common-validators/validate-map body {:destination_ids sequential?})
    (common-validators/validate-map params {:user string?})))

(with-post-hook! #'do-copy-metadata-template-avus (log-func "do-copy-metadata-template-avus"))
