(ns donkey.util.jwt
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [clojure-commons.jwt :as jwt]
            [clojure-commons.response :as resp]
            [donkey.util.config :as config]))

(def ^:private jwt-generator
  (memoize
   (fn []
     (jwt/generator (config/jwt-opts)))))

(def ^:private jwt-validator
  (memoize
   (fn []
     (jwt/validator (config/jwt-opts)))))

(defn jwt-user-from-donkey-user
  [donkey-user]
  {:user        (:shortUsername donkey-user)
   :email       (:email donkey-user)
   :given-name  (:firstName donkey-user)
   :family-name (:lastName donkey-user)
   :common-name (:commonName donkey-user)})

(defn donkey-user-from-jwt-user
  [jwt-user]
  {:shortUsername (:user jwt-user)
   :username      (str (:user jwt-user) "@" (config/uid-domain))
   :email         (:email jwt-user)
   :firstName     (:given-name jwt-user)
   :lastName      (:family-name jwt-user)
   :commonName    (:common-name jwt-user)})

(defn donkey-user-from-jwt-claims
  [jwt-claims]
  (donkey-user-from-jwt-user (jwt/user-from-assertion jwt-claims)))

(defn generate-jwt
  [user]
  ((jwt-generator) (jwt-user-from-donkey-user user)))

(defn add-auth-header
  ([user]
     (add-auth-header user {}))
  ([user headers]
     (assoc headers
       :X-Iplant-De-Jwt (generate-jwt user))))

(defn validate-group-membership
  [handler allowed-groups-fn]
  (fn [request]
    (let [allowed-groups (allowed-groups-fn)
          actual-groups  (get-in request [:jwt-claims :org.iplantc.de:entitlement] [])]
      (if (some (partial contains? (set allowed-groups)) actual-groups)
        (handler request)
        (resp/forbidden "You are not in one of the admin groups.")))))

(defn validate-jwt-assertion
  [handler assertion-fn]
  (fn [request]
    (try+
      (if-let [assertion (assertion-fn request)]
        (handler (assoc request :jwt-claims ((jwt-validator) assertion)))
        (resp/unauthorized "Custom JWT header not found."))
      (catch [:type :validation] _
          (resp/forbidden (.getMessage (:throwable &throw-context)))))))
