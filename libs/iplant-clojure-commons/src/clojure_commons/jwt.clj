(ns clojure-commons.jwt
  (:use [medley.core :only [remove-vals]])
  (:require [buddy.core.keys :as keys]
            [buddy.sign.jws :as jws]
            [clj-time.core :as time]))

(defn- build-assertion
  [after {:keys [user email given-name family-name common-name]}]
  (let [now (time/now)]
    (remove-vals nil?
                 {:sub         user
                  :exp         (time/plus now (time/seconds after))
                  :iat         now
                  :email       email
                  :given_name  given-name
                  :family_name family-name
                  :name        common-name})))

(defn generator
  [{:keys [after private-key-path private-key-password alg]}]
  (let [private-key (keys/private-key private-key-path private-key-password)]
    (fn [user]
      (jws/sign (build-assertion after user)
                private-key
                {:alg alg}))))

(defn validator
  [{:keys [public-key-path alg]}]
  (let [public-key (keys/public-key public-key-path)]
    (fn [assertion]
      (jws/unsign assertion public-key {:alg alg}))))
