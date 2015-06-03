(ns metadactyl.persistence.users
  (:use [kameleon.uuids :only [uuidify]]
        [korma.core]))

(defn- user-base-query
  []
  (-> (select* :users)
      (fields :id :username)))

(defn by-id
  [ids]
  (-> (user-base-query)
      (where {:id [in (mapv uuidify ids)]})
      (select)))
