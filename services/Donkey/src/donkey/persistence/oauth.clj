(ns donkey.persistence.oauth
  "Functions to use for storing and retrieving OAuth access tokens."
  (:use [donkey.auth.user-attributes :only [current-user]]
        [korma.core]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.db :as db])
  (:import [java.util Date]))

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
  [api-name username token-type expires-at refresh-token access-token]
  (update :access_tokens
          (set-fields {:token         access-token
                       :expires_at    expires-at
                       :refresh_token refresh-token})
          (where {:webapp  api-name
                  :user_id (user-id-subselect username)})))

(defn- insert-access-token
  "Inserts a new access token into the database."
  [api-name username token-type expires-at refresh-token access-token]
  (insert :access_tokens
          (values {:webapp        api-name
                   :user_id       (user-id-subselect username)
                   :token         access-token
                   :expires_at    expires-at
                   :refresh_token refresh-token})))

(defn- determine-expiration-time
  "Determines a token expiration time given its lifetime in seconds."
  [lifetime]
  (Date. (+ (System/currentTimeMillis) (* 1000 lifetime))))

(defn- extract-token-info
  "Extracts information from a token, performing conversions where necessary."
  [token-info]
  [(:token_type token-info)
   (determine-expiration-time (:expires_in token-info))
   (:refresh_token token-info)
   (:access_token token-info)])

(defn get-access-token
  "Retrieves an access code from the database."
  [api-name username]
  (first
   (select [:access_tokens :t]
           (join [:users :u] {:t.user_id :u.id})
           (fields [:t.webapp        :webapp]
                   [:t.expires_at    :expires-at]
                   [:t.refresh_token :refresh-token])
           (where {:u.username username
                   :a.webapp   api-name}))))

(defn has-access-token
  "Determines whether a user has an access token for an API."
  [api-name username]
  (seq (get-access-token api-name username)))

(defn store-access-token
  "Stores information about an OAuth access token in the database."
  [token-info api-name username]
  (let [[token-type expires-at refresh-token access-token] (extract-token-info token-info)]
    (validate-token-type (:token_type token-info))
    (if (has-access-token api-name username)
      (replace-access-token api-name username token-type expires-at refresh-token access-token)
      (insert-access-token api-name username token-type expires-at refresh-token access-token))))
