(ns metadactyl.persistence.oauth
  "Functions to use for storing and retrieving OAuth access tokens."
  (:use [korma.core :exclude [update]]
        [slingshot.slingshot :only [throw+]])
  (:require [korma.core :as sql]
            [metadactyl.util.pgp :as pgp])
  (:import [java.sql Timestamp]
           [java.util UUID]))

(defn- validate-token-type
  "Verifies that the token type is supported."
  [token-type]
  (when-not (= "bearer" token-type)
    (throw+ {:type  :clojure-commons.exception/illegal-argument
             :error (str "OAuth 2.0 token type, " token-type ", is not supported.")})))

(defn- user-id-subselect
  "Returns a subselect statement to find a user ID."
  [username]
  (subselect :users (fields :id) (where {:username username})))

(defn- replace-access-token
  "Replaces an existing access token in the database."
  [api-name username expires-at refresh-token access-token]
  (sql/update :access_tokens
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

(defn- determine-expiration-time
  "Determines a token expiration time given its lifetime in seconds."
  [lifetime]
  (Timestamp. (+ (System/currentTimeMillis) (* 1000 lifetime))))

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
  (validate-token-type token-type)
  (if (has-access-token api-name username)
    (replace-access-token api-name username expires-at refresh-token access-token)
    (insert-access-token api-name username expires-at refresh-token access-token)))

(defn- remove-prior-authorization-requests
  "Removes any previous OAuth authorization requests for the user."
  [username]
  (delete :authorization_requests
          (where {:user_id (user-id-subselect username)})))

(defn- insert-authorization-request
  "Inserts information about a new authorization request into the database."
  [id username state-info]
  (insert :authorization_requests
          (values {:id         id
                   :user_id    (user-id-subselect username)
                   :state_info state-info})))

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
  (first (select [:authorization_requests :r]
                 (join [:users :u] {:r.user_id :u.id})
                 (fields [:u.username :username]
                         [:r.state_info :state-info])
                 (where {:r.id id}))))

(defn retrieve-authorization-request-state
  "Retrieves an authorization request for a given UUID."
  [id username]
  (let [id  (if (string? id) (UUID/fromString id) id)
        req (get-authorization-request id)]
    (when (nil? req)
      (throw+ {:type  :clojure-commons.exception/bad-request-field
               :error (str "authorization request " (str id) " not found")}))
    (when (not= (:username req) username)
      (throw+ {:type  :clojure-commons.exception/bad-request-field
               :error (str "wrong user for authorization request " (str id))}))
    (remove-prior-authorization-requests username)
    (:state-info req)))
