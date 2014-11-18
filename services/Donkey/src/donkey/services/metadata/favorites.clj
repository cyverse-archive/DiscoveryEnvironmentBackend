(ns donkey.services.metadata.favorites
  (:require [clojure.set :as set]
            [cheshire.core :as json]
            [donkey.auth.user-attributes :as user]
            [donkey.clients.data-info :as data]
            [donkey.persistence.metadata :as db]
            [donkey.util.service :as svc]
            [donkey.util.validators :as valid])
  (:import [java.util UUID]))


(defn- format-favorites
  [favs]
  (letfn [(mk-fav [entry] (assoc entry :isFavorite true))]
    (assoc favs
      :files   (map mk-fav (:files favs))
      :folders (map mk-fav (:folders favs)))))


(defn- user-col->api-col
  [col]
  (if col
    (case (.toUpperCase col)
      "NAME"         :base-name
      "ID"           :full-path
      "LASTMODIFIED" :modify-ts
      "DATECREATED"  :create-ts
      "SIZE"         :data-size
                     :base-name)
    :base-name))


(defn- user-order->api-order
  [order]
  (if order
    (case (.toUpperCase order)
      "ASC"  :asc
      "DESC" :desc
             :asc)
    :asc))


(defn add-favorite
  "This function marks a given filesystem entry as a favorite of the authenticated user.

   Parameters:
     entry-id - This is the `entry-id` from the request.  It should be the UUID of the entry being
                marked."
  [entry-id]
  (let [user     (:shortUsername user/current-user)
        entry-id (UUID/fromString entry-id)]
    (data/validate-uuid-accessible user entry-id)
    (when-not (db/is-favorite? user entry-id)
      (db/insert-favorite user entry-id (data/resolve-data-type entry-id)))
    (svc/success-response)))


(defn remove-favorite
  "This function unmarks a given filesystem entry as a favortie of the authenticated user.

   Parameters:
     entry-id - This is the `entry-id` from the request.  It should be the UUID of the entry being
                unmarked."
  [entry-id]
  (let [user     (:shortUsername user/current-user)
        entry-id (UUID/fromString entry-id)]
  (if (db/is-favorite? user entry-id)
    (do
      (db/delete-favorite user entry-id)
      (svc/success-response))
    (svc/donkey-response {} 404))))


(defn- select-favorites
  [user entity-type]
  (let [entity-types (if (= :any entity-type)
                       ["file" "folder"]
                       [(name entity-type)])]
    (db/select-favorites-of-type user entity-types)))


(defn list-favorite-data-with-stat
  "Returns a listing of a user's favorite data, including stat information about it. This endpoint
   is intended to help with paginating.

   Parameters:
     sort-col    - This is the value of the `sort-col` query parameter. It should be a case-
                   insensitive string containing one of the following:
                   DATECREATED|ID|LASTMODIFIED|NAME|SIZE
     sort-order  - This is the value of the `sort-order` query parameter. It should be a case-
                   insensitive string containing one of the following: ASC|DESC
     limit       - This is the value of the `limit` query parameter. It should contain a positive
                   number.
     offset      - This is the value of the `offset` query parameter. It should contain a non-
                   negative number.
     entity-type - This is the value of the `entity-type` query parameter. It should be a case-
                   insensitive string containing one of the following: ANY|FILE|FOLDER. If it is
                   nil, ANY will be used.
     info-types  - This is the value(s) of the `info-type` query parameter(s). It may be nil,
                   meaning return all info types, a string containing a single info type, or a
                   sequence containing a set of info types."
  [sort-col sort-order limit offset entity-type info-types]
  (let [user        (:shortUsername user/current-user)
        col         (user-col->api-col sort-col)
        ord         (user-order->api-order sort-order)
        limit       (Long/valueOf limit)
        offset      (Long/valueOf offset)
        entity-type (valid/resolve-entity-type entity-type)
        uuids       (select-favorites user entity-type)]
    (->> (data/stats-by-uuids-paged user col ord limit offset uuids info-types)
      format-favorites
      (hash-map :filesystem)
      svc/success-response)))


(defn filter-favorites
  "Given a list of UUIDs for filesystem entries, it filters the list, returning only the UUIDS that
   are marked as favorite by the authenticated user.

   Parameters:
     body - This is the request body. It should contain a JSON document containing a field
            `filesystem` containing an array of UUIDs."
  [body]
  (let [user    (:shortUsername user/current-user)
        ids-txt (-> body slurp (json/parse-string true) :filesystem)
        entries (->> ids-txt (map #(UUID/fromString %)) set)]
    (->> (db/select-favorites-of-type user ["file" "folder"])
      (filter (partial data/uuid-accessible? user))
      set
      (set/intersection entries)
      (hash-map :filesystem)
      svc/success-response)))
