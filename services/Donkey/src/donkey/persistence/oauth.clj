(ns donkey.persistence.oauth
  "Functions to use for storing and retrieving OAuth access tokens."
  (:use [donkey.auth.user-attributes :only [current-user]]
        [korma.core]
        [korma.db :only [with-db]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.db :as db])
  (:import [java.sql Timestamp]))

(defn- validate-token-type
  "Verifies that the token type is supported."
  [token-type]
  (when-not (= "bearer" token-type)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     (str "OAuth 2.0 token type, " token-type ", is not supported.")})))

(defn- user-id-subselect
  "Returns a subselect statement to find a user ID."
  [username]
  (subselect :users (fields :id) (where {:username username})))

(defn- replace-access-token
  "Replaces an existing access token in the database."
  [api-name username expires-at refresh-token access-token]
  (with-db db/de
    (update :access_tokens
            (set-fields {:token         access-token
                         :expires_at    expires-at
                         :refresh_token refresh-token})
            (where {:webapp  api-name
                    :user_id (user-id-subselect username)}))))

(defn- insert-access-token
  "Inserts a new access token into the database."
  [api-name username expires-at refresh-token access-token]
  (with-db db/de
    (insert :access_tokens
            (values {:webapp        api-name
                     :user_id       (user-id-subselect username)
                     :token         access-token
                     :expires_at    expires-at
                     :refresh_token refresh-token}))))

(defn- determine-expiration-time
  "Determines a token expiration time given its lifetime in seconds."
  [lifetime]
  (Timestamp. (+ (System/currentTimeMillis) (* 1000 lifetime))))

(defn get-access-token
  "Retrieves an access code from the database."
  [api-name username]
  (with-db db/de
    (first
     (select [:access_tokens :t]
             (join [:users :u] {:t.user_id :u.id})
             (fields [:t.webapp        :webapp]
                     [:t.expires_at    :expires-at]
                     [:t.refresh_token :refresh-token])
             (where {:u.username username
                     :t.webapp   api-name})))))

(defn has-access-token
  "Determines whether a user has an access token for an API."
  [api-name username]
  (seq (get-access-token api-name username)))

(defn store-access-token
  "Stores information about an OAuth access token in the database."
  [api-name username {:keys [token-type expires-at refresh-token access-token]}]
  (validate-token-type token-type)
  (if (has-access-token api-name username)
    (replace-access-token api-name username expires-at refresh-token access-token)
    (insert-access-token api-name username expires-at refresh-token access-token)))
