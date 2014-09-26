(ns donkey.services.metadata.favorites
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [cheshire.core :as json]
            [clj-jargon.init :as fs]
            [donkey.auth.user-attributes :as user]
            [donkey.clients.data-info :as data]
            [donkey.persistence.metadata :as db]
            [donkey.util.config :as cfg]
            [donkey.util.service :as svc]
            [donkey.util.validators :as uv]
            [donkey.services.filesystem.icat :as icat])
  (:import [java.util UUID]))


(defn- format-favorites
  [favs]
  (let [favs (map #(assoc % :isFavorite true) favs)]
    {:folders (filter #(= (:type %) :dir) favs)
     :files   (filter #(= (:type %) :file) favs)}))


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
  (fs/with-jargon (cfg/jargon-cfg) [cm]
    (let [user     (:shortUsername user/current-user)
          entry-id (UUID/fromString entry-id)]
      (uv/validate-uuid-accessible user entry-id)
      (when-not (db/is-favorite? user entry-id)
        (db/insert-favorite user entry-id (icat/resolve-data-type cm entry-id)))
      (svc/success-response))))


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


(defn- compute-total
  "Computes the total number of favorites. If there are no favorites in the data store, but the
   total hasn't been reached, an error as logged and the data store accumulated total is returned."
  [uuids limit offset fav-page]
  (let [total      (count uuids)
        page-size  (count fav-page)
        page-accum (+ offset page-size)]
    (if (and (> limit page-size) (> total page-accum))
      (do
        (log/error "The metadata has favorites that are not in the data store.")
        page-accum)
      total)))


(defn list-favorite-data-with-stat
  "Returns a listing of a user's favorite data, including stat information about it. This endpoint
   is intended to help with paginating.

   Parameters:
     sort-col - This is the value of the `sort-col` query parameter. It should be a case-insensitive
                string containing one of the following: DATECREATED|ID|LASTMODIFIED|NAME|SIZE
     sort-order - This is the value of the `sort-order` query parameter. It should be a
                case-insensitive string containing one of the following: ASC|DESC
     limit - This is the value of the `limit` query parameter. It should contain a positive number.
     offset - This is the value of the `offset` query parameter. It should contain a non-negative
              number."
  [sort-col sort-order limit offset]
  (let [user         (:shortUsername user/current-user)
        col          (user-col->api-col sort-col)
        ord          (user-order->api-order sort-order)
        limit        (Long/valueOf limit)
        offset       (Long/valueOf offset)
        uuids        (db/select-favorites-of-type user ["file" "folder"])
        fav-page     (data/stats-by-uuids-paged user col ord limit offset uuids)
        attach-total (fn [favs] (assoc favs :total (compute-total uuids limit offset fav-page)))]
    (->> fav-page
      (format-favorites)
      attach-total
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
    (fs/with-jargon (cfg/jargon-cfg) [fs]
      (->> (db/select-favorites-of-type user ["file" "folder"])
        (filter (partial data/uuid-accessible? user))
        set
        (set/intersection entries)
        (hash-map :filesystem)
        svc/success-response))))
