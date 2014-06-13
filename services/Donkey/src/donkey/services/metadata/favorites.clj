(ns donkey.services.metadata.favorites
  (:require [clj-jargon.init :as fs]
            [donkey.persistence.metadata :as db]
            [donkey.services.filesystem.validators :as valid]
            [donkey.services.metadata.tags :as tag]
            [donkey.util.service :as svc]))



(defn add-favorite
  [fs-cfg user entry-id]
  (tag/validate-entry-accessible fs-cfg user entry-id)
  (when-not (db/is-favorite user entry-id)
    (db/insert-favorite user entry-id "data"))
  (svc/success-response))

(defn remove-favorite
  [user entry-id]
  (if (db/is-favorite user entry-id)
    (do
      (db/delete-favorite user entry-id)
      (svc/success-response))
    (svc/donkey-response {} 404)))

(defn list-favorite-data
  [fs-cfg user]
  (fs/with-jargon fs-cfg [fs]
    (valid/user-exists fs user)
    (->> (db/select-favorites-of-type user "data")
      (filter (partial tag/entry-accessible? fs user))
      (hash-map :filesystem)
      svc/success-response)))
