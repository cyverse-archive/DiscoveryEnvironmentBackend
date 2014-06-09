(ns donkey.services.metadata.tags
  (:require [cheshire.core :as json]
            [donkey.persistence.metadata :as db]
            [donkey.util.service :as svc]))


(defn create-user-tag
  [owner body]
  (let [tag         (json/parse-string (slurp body) true)
        value       (:value tag)
        description (:description tag)
        id          (:id (db/insert-user-tag owner value description))]
    (svc/success-response {:id id})))

#_(defn update-user-tag
  [owner value body]
  (let [req-updates     (json/parse-string (slurp body) true)
        new-value       (:value updates)
        new-description (:description updates)
        updates         (cond
                          (and new-value new-description) {:value       new-value
                                                           :description new-description}
                          new-value                       {:value new-value}
                          new-description                 {:description new-description}
                          ;; TODO handle no new value or description case
                          )]
    (db/update-user-tag owner value updates)
    (svc/success-response)))

#_(defn suggest-tags
  [user tag-prefix]
  (svc/success-response {:suggestions ["user/tag+1" "user/tag+2"]}))

