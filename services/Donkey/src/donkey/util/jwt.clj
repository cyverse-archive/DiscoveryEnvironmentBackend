(ns donkey.util.jwt
  (:use [donkey.auth.user-attributes :only [current-user]])
  (:require [clojure-commons.jwt :as jwt]
            [donkey.util.config :as config]))

(def ^:private jwt-generator
  (memoize
   (fn []
     (jwt/generator (config/jwt-opts)))))

(defn- jwt-user-from-donkey-user
  [donkey-user]
  {:user        (:shortUsername donkey-user)
   :email       (:email donkey-user)
   :given-name  (:firstName donkey-user)
   :family-name (:lastName donkey-user)
   :common-name (:commonName donkey-user)})

(defn- donkey-user-from-jwt-user
  [jwt-user]
  {:shortUsername (:user jwt-user)
   :username      (:str (:user jwt-user) "@" (config/uid-domain))
   :email         (:email jwt-user)
   :firstName     (:given-name jwt-user)
   :lastName      (:family-name jwt-user)
   :commonName    (:common-name jwt-user)})

(defn generate-jwt
  ([]
     (generate-jwt current-user))
  ([user]
     ((jwt-generator) (jwt-user-from-donkey-user user))))

(defn add-auth-header
  ([]
     (add-auth-header current-user {}))
  ([user]
     (add-auth-header user {}))
  ([user headers]
     (assoc headers
       :X-Iplant-De-Jwt (generate-jwt user))))
