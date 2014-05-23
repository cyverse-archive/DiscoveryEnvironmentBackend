(ns donkey.persistence.oauth
  "Functions to use for storing and retrieving OAuth access tokens."
  (:use [donkey.auth.user-attributes :only [current-user]]
        [korma.core]
        [korma.db :only [with-db]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.db :as db])
  (:import [java.sql Timestamp]
           [java.util UUID]))

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

(defn- remove-prior-authorization-requests
  "Removes any previous OAuth authorization requests for the user."
  [username]
  (with-db db/de
    (delete :authorization_requests
            (where {:user_id (user-id-subselect username)}))))

(defn- insert-authorization-request
  "Inserts information about a new authorization request into the database."
  [id username state-info]
  (with-db db/de
    (insert :authorization_requests
            (values {:id         id
                     :user_id    (user-id-subselect username)
                     :state_info state-info}))))

(defn store-authorization-request
  "Stores state information for an OAuth authorization request."
  [username state-info]
  (let [id (UUID/randomUUID)]
    (remove-prior-authorization-requests username)
    (insert-authorization-request id username state-info)
    (str id)))

(defn- get-authorization-request
  "Gets authorization request information from the database."
  [id]
  (with-db db/de
    (first (select [:authorization_requests :r]
                   (join [:users :u] {:r.user_id :u.id})
                   (fields [:u.username :username]
                           [:r.state_info :state-info])))))

(defn retrieve-authorization-request-state
  "Retrieves an authorization request for a given UUID."
  [id username]
  (let [id  (if (string? id) (UUID/fromString id) id)
        req (get-authorization-request id)]
    (when (nil? req)
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     (str "authorization request " (str id) " not found")}))
    (when (not= (:username req) username)
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     (str "wrong user for authorization request " (str id))}))
    (remove-prior-authorization-requests username)
    (:state-info req)))
