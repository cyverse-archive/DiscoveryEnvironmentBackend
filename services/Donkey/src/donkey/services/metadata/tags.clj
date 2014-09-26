(ns donkey.services.metadata.tags
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.set :as set]
            [cheshire.core :as json]
            [clojure-commons.error-codes :as error]
            [donkey.auth.user-attributes :as user]
            [donkey.persistence.metadata :as db]
            [donkey.util.service :as svc]
            [donkey.util.validators :as valid]
            [donkey.services.filesystem.icat :as icat])
(:import [java.util UUID]))


(defn- attach-tags
  [user entry-id new-tags]
  (let [tag-set         (set new-tags)
        known-tags      (set (db/filter-tags-owned-by-user user tag-set))
        unknown-tags    (set/difference tag-set known-tags)
        unattached-tags (set/difference known-tags
                                        (set (db/filter-attached-tags entry-id known-tags)))]
    (valid/validate-uuid-accessible user entry-id)
    (when-not (empty? unknown-tags)
      (throw+ {:error_code error/ERR_NOT_FOUND :tag-ids unknown-tags}))
    (db/insert-attached-tags user entry-id (icat/resolve-data-type entry-id) unattached-tags)
    (svc/success-response)))


(defn- detach-tags
  [user entry-id tag-ids]
  (valid/validate-uuid-accessible user entry-id)
  (db/mark-tags-detached user entry-id (db/filter-tags-owned-by-user user (set tag-ids)))
  (svc/success-response))


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
    (if (empty? (db/get-tags-by-value owner value))
      (let [id (:id (db/insert-user-tag owner value description))]
        (svc/success-response {:id id}))
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
        tag-owner (db/get-tag-owner tag-id)]
    (when (not= tag-owner (:shortUsername user/current-user))
      (throw+ {:error_code error/ERR_NOT_FOUND :tag-id tag-id}))
    (db/delete-user-tag tag-id)
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
        user     (:shortUsername user/current-user)]
    (condp = type
      "attach" (attach-tags user entry-id mods)
      "detach" (detach-tags user entry-id mods)
      (svc/donkey-response {} 400))))


(defn list-attached-tags
  "Lists the tags attached to a filesystem entry.

   Parameters:
     entry-id - The entry-id from the request.  It should be a filesystem UUID."
  [entry-id]
  (let [user     (:shortUsername user/current-user)
        entry-id (UUID/fromString entry-id)
        tags     (db/select-attached-tags user entry-id)]
    (valid/validate-uuid-accessible user entry-id)
    (svc/success-response {:tags (map #(dissoc % :owner_id) tags)})))


(defn suggest-tags
  "Given a tag value fragment, this function will return a list tags whose values contain that
   fragment.

   Parameters:
     contains - The `contains` query parameter.
     limit - The `limit` query parameter. It should be a positive integer."
  [contains limit]
  (let [matches (db/get-tags-by-value (:shortUsername user/current-user)
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
              (when updates (db/update-user-tag (UUID/fromString tag-id) updates))
              (svc/success-response)))]
    (let [owner     (:shortUsername user/current-user)
          tag-owner (db/get-tag-owner (UUID/fromString tag-id))]
      (cond
        (nil? tag-owner)       (svc/donkey-response {} 404)
        (not= owner tag-owner) (svc/donkey-response {} 403)
        :else                  (do-update)))))
