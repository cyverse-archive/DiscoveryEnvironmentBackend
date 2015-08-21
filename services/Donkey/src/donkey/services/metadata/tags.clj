(ns donkey.services.metadata.tags
  (:use [kameleon.uuids :only [uuidify]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.set :as set]
            [cheshire.core :as json]
            [clojure-commons.error-codes :as error]
            [donkey.auth.user-attributes :as user]
            [donkey.clients.data-info :as data]
            [donkey.clients.metadata.raw :as meta]
            [donkey.persistence.search :as search]
            [donkey.util.config :as config]
            [donkey.util.service :as svc]
            [donkey.util.validators :as valid])
  (:import [java.io Reader]
           [java.util UUID]
           [clojure.lang IPersistentMap]))


(defn- update-tags-targets
  [response]
  (letfn [(fmt-tgt ([tgt] (update tgt :id uuidify)))
          (parse-resp ([response] (-> response :body slurp (json/parse-string true) :tags)))]
    (doseq [{:keys [id targets]} (parse-resp response)]
      (search/update-tag-targets (uuidify id) (map fmt-tgt targets)))))


(defn- format-new-tag-doc
  [db-tag]
  {:id           (:id db-tag)
   :value        (:value db-tag)
   :description  (:description db-tag)
   :creator      (str (:owner_id db-tag) \# (config/irods-zone))
   :dateCreated  (:created_on db-tag)
   :dateModified (:modified_on db-tag)
   :targets      []})


(defn ^IPersistentMap create-user-tag
  "Creates a new user tag

   Parameters:
     body - This is the request body. It should be a JSON document containing a `value` text field
            and optionally a `description` text field.

   Returns:
     It returns the response."
  [^String body]
  (let [tag (-> body slurp meta/create-user-tag :body slurp (json/parse-string true))]
    (search/index-tag (format-new-tag-doc tag))
    (svc/success-response (select-keys tag [:id]))))


(defn ^IPersistentMap delete-user-tag
  "Deletes a user tag. This will detach it from all metadata.

   Parameters:
     tag-id - The tag's UUID from the URL

   Returns:
     A success response with no body.

   Throws:
     ERR_NOT_FOUND - if the text isn't a UUID owned by the authenticated user."
  [^UUID tag-id]
  (let [tag-id (valid/extract-uri-uuid tag-id)]
    (meta/delete-user-tag tag-id)
    (search/remove-tag tag-id)
    (svc/success-response)))


(defn handle-patch-file-tags
  "Adds or removes tags to a filesystem entry.

   Parameters:
     entry-id - The entry-id from the request. It should be a filesystem UUID.
     type - The `type` query parameter. It should be either `attach` or `detach`.
     body - This is the request body. It should be a JSON document containing a `tags` field. This
            field should hold an array of tag UUIDs."
  [entry-id type body]
  (let [entry-id (uuidify entry-id)
        req      (slurp body)
        user     (:shortUsername user/current-user)]
    (data/validate-uuid-accessible user entry-id)
    (update-tags-targets
      (meta/update-attached-tags entry-id (data/resolve-data-type entry-id) type req))
    (svc/success-response)))


(defn list-attached-tags
  "Lists the tags attached to a filesystem entry.

   Parameters:
     entry-id - The entry-id from the request.  It should be a filesystem UUID."
  [entry-id]
  (let [user     (:shortUsername user/current-user)
        entry-id (uuidify entry-id)]
    (data/validate-uuid-accessible user entry-id)
    (meta/list-attached-tags entry-id)))


(defn suggest-tags
  "Given a tag value fragment, this function will return a list tags whose values contain that
   fragment.

   Parameters:
     contains - The `contains` query parameter.
     limit - The `limit` query parameter. It should be a positive integer."
  [contains limit]
  (meta/get-tags-by-value contains limit))


(defn- do-update-tag
  [tag-id tag-rec]
  (let [doc-updates {:value        (:value tag-rec)
                     :description  (:description tag-rec)
                     :dateModified (:modified_on tag-rec)}]
    (search/update-tag tag-id doc-updates)))


(defn ^IPersistentMap update-user-tag
  "updates the value and/or description of a tag.

   Parameters:
     tag-str - The tag-id from request URL. It should be a tag UUID.
     body    - The request body. It should be a JSON document containing at most one `value` text
               field and one `description` text field.

    Returns:
      It returns the response."
  [^String tag-str ^Reader body]
  (let [tag-id  (uuidify tag-str)
        update  (meta/update-user-tag tag-id (slurp body))
        tag-rec (-> update :body slurp (json/parse-string true))]
    (do-update-tag tag-id tag-rec)
    (svc/success-response {})))
