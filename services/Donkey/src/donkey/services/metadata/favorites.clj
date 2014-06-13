(ns donkey.services.metadata.favorites
  (:require [donkey.persistence.metadata :as db]
            [donkey.services.metadata.tags :as tag]
            [donkey.util.service :as svc]))

(defn add-favorite
  [fs-cfg user entry-id]
  (tag/validate-entry-accessible fs-cfg user entry-id)
  (when-not (db/is-favorite user entry-id)
    (db/insert-favorite user entry-id "data"))
  (svc/success-response))
