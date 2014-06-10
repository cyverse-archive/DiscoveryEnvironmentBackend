(ns donkey.services.metadata.tags
  (:require [cheshire.core :as json]
            [donkey.persistence.metadata :as db]
            [donkey.util.service :as svc]))


(defn create-user-tag
  [owner body]
  (let [tag         (json/parse-string (slurp body) true)
        value       (:value tag)
        description (:description tag)]
    (if (empty? (db/get-tag-by-value owner value))
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

#_(defn suggest-tags
  [user tag-prefix]
  (svc/success-response {:suggestions ["user/tag+1" "user/tag+2"]}))

