(ns donkey.auth.user-attributes
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clojure-commons.response :as resp]
            [clj-cas.cas-proxy-auth :as cas]
            [clojure-commons.exception :as cx]
            [donkey.util.config :as cfg]
            [donkey.util.jwt :as jwt]))


(def
  ^{:doc "The authenticated user or nil if the service is unsecured."
    :dynamic true}
  current-user nil)

;; TODO: fix common name retrieval when we add it as an attribute to CAS.
(defn user-from-attributes
  "Creates a map of values from user attributes stored in the request by
   validate-cas-proxy-ticket."
  [{:keys [user-attributes]}]
  (log/trace user-attributes)
  (let [first-name (get user-attributes "firstName")
        last-name  (get user-attributes "lastName")]
    {:username      (str (get user-attributes "uid") "@" (cfg/uid-domain)),
     :password      (get user-attributes "password"),
     :email         (get user-attributes "email"),
     :shortUsername (get user-attributes "uid")
     :firstName     first-name
     :lastName      last-name
     :commonName    (str first-name " " last-name)
     :principal     (get user-attributes "principal")}))

(defn user-from-jwt-claims
  "Creates a map of values from JWT claims stored in the request."
  [{:keys [jwt-claims]}]
  (jwt/donkey-user-from-jwt-claims jwt-claims))

(defn fake-user-from-attributes
  "Creates a real map of fake values for a user base on environment variables."
  [& _]
  {:username      (System/getenv "IPLANT_CAS_USER")
   :password      (System/getenv "IPLANT_CAS_PASS")
   :email         (System/getenv "IPLANT_CAS_EMAIL")
   :shortUsername (System/getenv "IPLANT_CAS_SHORT")
   :firstName     (System/getenv "IPLANT_CAS_FIRST")
   :lastName      (System/getenv "IPLANT_CAS_LAST")
   :commonName    (System/getenv "IPLANT_CAS_COMMON")})

(defn- user-info-from-current-user
  "Converts the current-user to the user info structure expected in the request."
  [user]
  {:user       (:shortUsername user)
   :email      (:email user)
   :first-name (:firstName user)
   :last-name  (:lastName user)})

(defn wrap-current-user
  "Generates a Ring handler function that stores user information in current-user."
  [handler user-info-fn]
  (fn [request]
    (binding [current-user (user-info-fn request)]
      (handler (assoc request :user-info (user-info-from-current-user current-user))))))

(defn- find-auth-handler
  "Finds an authentication handler for a request."
  [request phs]
  (->> (remove (fn [[token-fn _]] (nil? (token-fn request))) phs)
       (first)
       (second)))

(defn- wrap-auth-selection
  "Generates a ring handler function that selects the authentication method based on predicates."
  [phs]
  (fn [request]
    (log/log 'AccessLogger :trace nil "entering donkey.auth.user-attributes/wrap-auth-selection")
    (if-let [auth-handler (find-auth-handler request phs)]
      (auth-handler request)
      (resp/unauthorized "No authentication information found in request."))))

(defn- get-fake-auth
  "Returns a non-nil value if we're using fake authentication."
  [_]
  (System/getenv "IPLANT_CAS_FAKE"))

(defn- get-cas-ticket
  "Extracts a CAS ticket from the request, returning nil if none is found."
  [request]
  (get (:query-params request) "proxyToken"))

(defn- get-jwt-assertion
  "Extracts a JWT assertion from the request, returning nil if none is found."
  [request]
  (get (:headers request) "x-iplant-de-jwt"))

(defn- wrap-fake-auth
  [handler]
  (wrap-current-user handler fake-user-from-attributes))

(defn- wrap-cas-auth
  [handler]
  (-> (wrap-current-user handler user-from-attributes)
      (cas/extract-groups-from-user-attributes cfg/group-attr-name)
      (cas/validate-cas-proxy-ticket
        get-cas-ticket cfg/cas-server cfg/server-name)))

(defn- wrap-jwt-auth
  [handler]
  (-> (wrap-current-user handler user-from-jwt-claims)
      (jwt/validate-jwt-assertion get-jwt-assertion)))

(defn authenticate-current-user
  "Authenticates the user using validate-cas-proxy-ticket and binds current-user to a map that is
   built from the user attributes that validate-cas-proxy-ticket stores in the request."
  [handler]
  (wrap-auth-selection [[get-fake-auth     (wrap-fake-auth handler)]
                        [get-cas-ticket    (wrap-cas-auth handler)]
                        [get-jwt-assertion (wrap-jwt-auth handler)]]))

(defn validate-current-user
  "Verifies that the user belongs to one of the groups that are permitted to access the resource."
  [handler]
  (wrap-auth-selection
   [[get-fake-auth     handler]
    [get-cas-ticket    (cas/validate-group-membership handler cfg/allowed-groups)]
    [get-jwt-assertion (jwt/validate-group-membership handler cfg/allowed-groups)]]))

(defn fake-store-current-user
  "Fake storage of a user"
  [handler & _]
  (fn [req]
    (log/info "Storing current user from IPLANT_CAS_* env vars.")
    (binding [current-user (fake-user-from-attributes req)]
      (handler req))))

(defmacro with-user
  "Performs a task with the given user information bound to current-user. This macro is used
   for debugging in the REPL."
  [[user] & body]
  `(binding [current-user (user-from-attributes {:user-attributes ~user})]
     (do ~@body)))
