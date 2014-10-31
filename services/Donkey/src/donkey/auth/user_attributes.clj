(ns donkey.auth.user-attributes
  (:use [donkey.util.config])
  (:require [clj-cas.cas-proxy-auth :as cas]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [donkey.clients.user-info :as du]))

(def
  ^{:doc "The authenticated user or nil if the service is unsecured."
    :dynamic true}
  current-user nil)

(defn user-from-attributes
  "Creates a map of values from user attributes stored in the request by
   validate-cas-proxy-ticket."
  [{:keys [user-attributes]}]
  (log/trace user-attributes)
  {:username      (str (get user-attributes "uid") "@" (uid-domain)),
   :password      (get user-attributes "password"),
   :email         (get user-attributes "email"),
   :shortUsername (get user-attributes "uid")
   :firstName     (get user-attributes "firstName")
   :lastName      (get user-attributes "lastName")
   :principal     (get user-attributes "principal")})

(defn user-from-user-info
  "Creates a map of values from user attributes retrieved from the user info service."
  [username]
  (let [short-username (string/replace username #"@.*" "")
        user-info      (du/get-user-details short-username)]
    {:username      username
     :password      nil
     :email         (:email user-info)
     :shortUsername short-username
     :firstName     (:firstname user-info)
     :lastName      (:lastname user-info)}))

(defn fake-user-from-attributes
  "Creates a real map of fake values for a user base on environment variables."
  [placeholder & args]
  {:username      (System/getenv "IPLANT_CAS_USER")
   :password      (System/getenv "IPLANT_CAS_PASS")
   :email         (System/getenv "IPLANT_CAS_EMAIL")
   :shortUsername (System/getenv "IPLANT_CAS_SHORT")
   :firstName     (System/getenv "IPLANT_CAS_FIRST")
   :lastName      (System/getenv "IPLANT_CAS_LAST")})

(defn store-current-admin-user
  "Authenticates the user using validate-cas-group-membership and binds current-user to a map that
   is built from the user attributes that validate-cas-proxy-ticket stores in the request."
  [handler cas-server-fn server-name-fn group-attr-name-fn allowed-groups-fn
   pgt-callback-base-fn pgt-callback-path-fn]
  (cas/validate-cas-group-membership
    (fn [request]
      (binding [current-user (user-from-attributes request)]
        (handler request)))
    cas-server-fn server-name-fn group-attr-name-fn allowed-groups-fn
    pgt-callback-base-fn pgt-callback-path-fn))

(defn store-current-user
  "Authenticates the user using validate-cas-proxy-ticket and binds
   current-user to a map that is built from the user attributes that
   validate-cas-proxy-ticket stores in the request."
  [handler cas-server-fn server-name-fn pgt-callback-base-fn pgt-callback-path-fn]
  (cas/validate-cas-proxy-ticket
   (fn [request]
     (binding [current-user (user-from-attributes request)]
       (handler request)))
   cas-server-fn server-name-fn pgt-callback-base-fn pgt-callback-path-fn))

(defn fake-store-current-user
  "Fake storage of a user"
  [handler & cas-config-fns]
  (log/debug "Storing current user from IPLANT_CAS_* env vars.")
  (fn [req]
    (binding [current-user (fake-user-from-attributes req)]
      (handler req))))

(defn get-proxy-ticket
  "Obtains a CAS proxy ticket for authentication to another service."
  [url]
  (cas/get-proxy-ticket (:principal current-user) url))

(defmacro with-user
  "Performs a task with the given user information bound to current-user. This macro is used
   for debugging in the REPL."
  [[user] & body]
  `(binding [current-user (user-from-attributes {:user-attributes ~user})]
     (do ~@body)))

(defmacro with-directory-user
  "Performs a task with user information for the given username bound to current-user. This
   macro is used for callback endpoints."
  [[username] & body]
  `(binding [current-user (user-from-user-info ~username)]
     (do ~@body)))
