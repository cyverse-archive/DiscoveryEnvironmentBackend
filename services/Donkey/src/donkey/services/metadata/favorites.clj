(ns donkey.services.metadata.favorites
  (:require [clojure.set :as set]
            [clj-jargon.init :as fs]
            [donkey.persistence.metadata :as db]
            [donkey.services.filesystem.uuids :as uuids]
            [donkey.services.filesystem.validators :as valid]
            [donkey.services.metadata.tags :as tag]
            [donkey.util.service :as svc]
            [clojure.tools.logging :as log]))


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

(defn- favorite-data
  [user]
  (->> (db/select-favorites-of-type user "data")
       (map str)
       (uuids/paths-for-uuids user)))

(defn- format-favorites
  [favs]
  (let [favs (map #(assoc % :isFavorite true) favs)]
    {:folders (filter #(= (:type %) :dir) favs)
     :files (filter #(= (:type %) :file) favs)}))

(defn list-favorite-data-with-stat
  "Returns a listing of a user's favorite data, including stat information about it."
  [fs-cfg user]
  (fs/with-jargon fs-cfg [fs]
    (->> (favorite-data user)
         (format-favorites)
         (hash-map :filesystem)
         svc/success-response)))

(defn filter-favorites
  [fs-cfg user entries]
  (fs/with-jargon fs-cfg [fs]
    (valid/user-exists fs user)
    (->> (db/select-favorites-of-type user "data")
      (filter (partial tag/entry-accessible? fs user))
      set
      (set/intersection (set entries))
      (hash-map :filesystem)
      svc/success-response)))


