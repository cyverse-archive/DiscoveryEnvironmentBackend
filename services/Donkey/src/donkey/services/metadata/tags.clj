(ns donkey.services.metadata.tags
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.set :as set]
            [cheshire.core :as json]
            [clj-jargon.init :as fs-init]
            [clj-jargon.permissions :as fs-perm]
            [clojure-commons.error-codes :as error]
            [donkey.persistence.metadata :as db]
            [donkey.services.filesystem.uuids :as uuid]
            [donkey.services.filesystem.validators :as valid]
            [donkey.util.service :as svc]))


;; TODO move this some place special
(defn entry-accessible?
  [fs user entry-id]
  (let [entry-path (:path (uuid/path-for-uuid fs user (str entry-id)))]
    (and entry-path (fs-perm/is-readable? fs user entry-path))))


;; TODO move this some place special
(defn validate-entry-accessible
  [fs-cfg user entry-id]
  (fs-init/with-jargon fs-cfg [fs]
    (valid/user-exists fs user)
    (if-not (entry-accessible? fs user entry-id)
      (throw+ {:error_code error/ERR_NOT_FOUND :uuid entry-id}))))


(defn create-user-tag
  [owner body]
  (let [tag         (json/parse-string (slurp body) true)
        value       (:value tag)
        description (:description tag)]
    (if (empty? (db/get-tags-by-value owner value))
      (let [id (:id (db/insert-user-tag owner value description))]
        (svc/success-response {:id id}))
      (svc/donkey-response {} 409))))


(defn update-user-tag
  [owner tag-id body]
  (letfn [(do-update []
            (let [req-updates     (json/parse-string (slurp body) true)
                  new-value       (:value req-updates)
                  new-description (:description req-updates)
                  updates         (cond
                                    (and new-value new-description) {:value       new-value
                                                                     :description new-description}
                                    new-value                       {:value new-value}
                                    new-description                 {:description new-description})]
              (when updates (db/update-user-tag tag-id updates))
              (svc/success-response)))]
    (let [tag (first (db/get-tag tag-id))]
      (cond
        (empty? tag)                 (svc/donkey-response {} 404)
        (not= owner (:owner_id tag)) (svc/donkey-response {} 403)
        :else                        (do-update)))))


(defn suggest-tags
  [user value-part]
  (let [matches (db/get-tags-by-value user (str "%" value-part "%"))]
  (svc/success-response {:suggestions (map #(dissoc % :owner_id) matches)})))


(defn list-attached-tags
  [fs-cfg user entry-id]
  (validate-entry-accessible fs-cfg user entry-id)
  (let [tags (db/select-attached-tags user entry-id)]
    (svc/success-response {:tags (map #(dissoc % :owner_id) tags)})))


(defn attach-tags
  [fs-cfg user entry-id new-tags]
  (validate-entry-accessible fs-cfg user entry-id)
  (let [tag-set         (set new-tags)
        known-tags      (set (db/filter-tags-owned-by-user user tag-set))
        unknown-tags    (set/difference tag-set known-tags)
        unattached-tags (set/difference known-tags
                                        (set (db/filter-attached-tags entry-id known-tags)))]
    (when-not (empty? unknown-tags)
      (throw+ {:error_code error/ERR_NOT_FOUND :tag-ids unknown-tags}))
    (db/insert-attached-tags user entry-id "data" unattached-tags)
    (svc/success-response)))


(defn detach-tags
  [fs-cfg user entry-id tag-ids]
  (validate-entry-accessible fs-cfg user entry-id)
  (db/mark-tags-detached user entry-id (db/filter-tags-owned-by-user user (set tag-ids)))
  (svc/success-response))