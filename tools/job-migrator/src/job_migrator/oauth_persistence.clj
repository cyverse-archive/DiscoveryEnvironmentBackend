(ns job-migrator.oauth-persistence
  (:use [korma.core])
  (:require [job-migrator.pgp :as pgp]))

(defn- user-id-subselect
  "Returns a subselect statement to find a user ID."
  [username]
  (subselect :users (fields :id) (where {:username username})))

(defn- replace-access-token
  "Replaces an existing access token in the database."
  [api-name username expires-at refresh-token access-token]
  (update :access_tokens
          (set-fields {:token         (pgp/encrypt access-token)
                       :expires_at    expires-at
                       :refresh_token (pgp/encrypt refresh-token)})
          (where {:webapp  api-name
                  :user_id (user-id-subselect username)})))

(defn- insert-access-token
  "Inserts a new access token into the database."
  [api-name username expires-at refresh-token access-token]
  (insert :access_tokens
          (values {:webapp        api-name
                   :user_id       (user-id-subselect username)
                   :token         (pgp/encrypt access-token)
                   :expires_at    expires-at
                   :refresh_token (pgp/encrypt refresh-token)})))

(defn- decrypt-tokens
  "Decrypts access and refresh tokens retrieved from the database."
  [token-info]
  (when-not (nil? token-info)
    (-> token-info
        (update-in [:access-token] pgp/decrypt)
        (update-in [:refresh-token] pgp/decrypt))))

(defn get-access-token
  "Retrieves an access code from the database."
  [api-name username]
  (->> (select [:access_tokens :t]
               (join [:users :u] {:t.user_id :u.id})
               (fields [:t.webapp        :webapp]
                       [:t.expires_at    :expires-at]
                       [:t.refresh_token :refresh-token]
                       [:t.token         :access-token])
               (where {:u.username username
                       :t.webapp   api-name}))
       (first)
       (decrypt-tokens)))

(defn has-access-token
  "Determines whether a user has an access token for an API."
  [api-name username]
  (seq (get-access-token api-name username)))

(defn store-access-token
  "Stores information about an OAuth access token in the database."
  [api-name username {:keys [token-type expires-at refresh-token access-token]}]
  (if (has-access-token api-name username)
    (replace-access-token api-name username expires-at refresh-token access-token)
    (insert-access-token api-name username expires-at refresh-token access-token)))
