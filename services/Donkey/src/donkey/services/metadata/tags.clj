(ns donkey.services.metadata.tags
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.set :as set]
            [cheshire.core :as json]
            [clj-jargon.init :as fs-init]
            [clojure-commons.error-codes :as error]
            [donkey.auth.user-attributes :as user]
            [donkey.persistence.metadata :as meta]
            [donkey.persistence.search :as search]
            [donkey.services.filesystem.uuids :as uuids]
            [donkey.util.config :as config]
            [donkey.util.icat :as icat]
            [donkey.util.service :as svc]
            [donkey.util.validators :as valid])
  (:import [java.util UUID]))


(defn- validate-entry-accessible
  [fs-cfg user entry-id]
  (fs-init/with-jargon fs-cfg [fs]
    (uuids/validate-uuid-accessible fs user entry-id)))


(defn- attach-tags
  [fs-cfg user entry-id new-tags]
  (fs-init/with-jargon fs-cfg [fs]
    (let [tag-set         (set new-tags)
          known-tags      (set (meta/filter-tags-owned-by-user user tag-set))
          unknown-tags    (set/difference tag-set known-tags)
          unattached-tags (set/difference known-tags
                                          (set (meta/filter-attached-tags entry-id known-tags)))]
      (uuids/validate-uuid-accessible fs user entry-id)
      (when-not (empty? unknown-tags)
        (throw+ {:error_code error/ERR_NOT_FOUND :tag-ids unknown-tags}))
      (meta/insert-attached-tags user entry-id (icat/resolve-data-type fs entry-id) unattached-tags)
      (svc/success-response))))


(defn- detach-tags
  [fs-cfg user entry-id tag-ids]
  (validate-entry-accessible fs-cfg user entry-id)
  (meta/mark-tags-detached user entry-id (meta/filter-tags-owned-by-user user (set tag-ids)))
  (svc/success-response))


(defn- format-new-tag-doc
  [db-tag]
  {:id           (:id db-tag)
   :value        (:value db-tag)
   :description  (:description db-tag)
   :creator      (:owner_id db-tag)
   :dateCreated  (:created_on db-tag)
   :dateModified (:modified_on db-tag)
   :targets      []})


(defn create-user-tag
  "Creates a new user tag

   Parameters:
     body - This is the request body. It should be a JSON document containing a `value` text field
            and optionally a `description` text field."
  [body]
  (let [owner       (:shortUsername user/current-user)
        tag         (json/parse-string (slurp body) true)
        value       (:value tag)
        description (:description tag)]
    (if (empty? (meta/get-tags-by-value owner value))
      (let [db-tag (meta/insert-user-tag owner value description)]
        (search/index-tag (format-new-tag-doc db-tag))
        (svc/success-response (select-keys db-tag [:id])))
      (svc/donkey-response {} 409))))


(defn delete-user-tag
  "Deletes a user tag. This will detach it from all metadata.

   Parameters:
     tag-id - The tag's UUID from the URL

   Returns:
     A success response with no body.

   Throws:
     ERR_NOT_FOUND - if the text isn't a UUID owned by the authenticated user."
  [tag-id]
  (let [tag-id    (valid/extract-uri-uuid tag-id)
        tag-owner (meta/get-tag-owner tag-id)]
    (when (not= tag-owner (:shortUsername user/current-user))
      (throw+ {:error_code error/ERR_NOT_FOUND :tag-id tag-id}))
    (meta/delete-user-tag tag-id)
    (svc/success-response)))


(defn handle-patch-file-tags
  "Adds or removes tags to a filesystem entry.

   Parameters:
     entry-id - The entry-id from the request. It should be a filesystem UUID.
     type - The `type` query parameter. It should be either `attach` or `detach`.
     body - This is the request body. It should be a JSON document containing a `tags` field. This
            field should hold an array of tag UUIDs."
  [entry-id type body]
  (let [entry-id (UUID/fromString entry-id)
        req      (-> body slurp (json/parse-string true))
        mods     (map #(UUID/fromString %) (:tags req))
        user     (:shortUsername user/current-user)
        fs-cfg   (config/jargon-cfg)]
    (condp = type
      "attach" (attach-tags fs-cfg user entry-id mods)
      "detach" (detach-tags fs-cfg user entry-id mods)
      (svc/donkey-response {} 400))))


(defn list-attached-tags
  "Lists the tags attached to a filesystem entry.

   Parameters:
     entry-id - The entry-id from the request.  It should be a filesystem UUID."
  [entry-id]
  (let [user     (:shortUsername user/current-user)
        entry-id (UUID/fromString entry-id)
        tags     (meta/select-attached-tags user entry-id)]
    (validate-entry-accessible (config/jargon-cfg) user entry-id)
    (svc/success-response {:tags (map #(dissoc % :owner_id) tags)})))


(defn suggest-tags
  "Given a tag value fragment, this function will return a list tags whose values contain that
   fragment.

   Parameters:
     contains - The `contains` query parameter.
     limit - The `limit` query parameter. It should be a positive integer."
  [contains limit]
  (let [matches (meta/get-tags-by-value (:shortUsername user/current-user)
                                        (str "%" contains "%")
                                        (Long/valueOf limit))]
    (svc/success-response {:tags (map #(dissoc % :owner_id) matches)})))


(defn update-user-tag
  "updates the value and/or description of a tag.

   Parameters:
     tag-id - The tag-id from request URL. It should be a tag UUID.
     body - The request body. It should be a JSON document containing at most one `value` text field
            and one `description` text field."
  [tag-id body]
  (letfn [(do-update []
            (let [req-updates     (json/parse-string (slurp body) true)
                  new-value       (:value req-updates)
                  new-description (:description req-updates)
                  updates         (cond
                                    (and new-value new-description) {:value       new-value
                                                                     :description new-description}
                                    new-value                       {:value new-value}
                                    new-description                 {:description new-description})]
              (when updates (meta/update-user-tag (UUID/fromString tag-id) updates))
              (svc/success-response)))]
    (let [owner     (:shortUsername user/current-user)
          tag-owner (meta/get-tag-owner (UUID/fromString tag-id))]
      (cond
        (nil? tag-owner)       (svc/donkey-response {} 404)
        (not= owner tag-owner) (svc/donkey-response {} 403)
        :else                  (do-update)))))
