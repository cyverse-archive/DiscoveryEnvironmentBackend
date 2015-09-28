(ns clojure-commons.jwt
  (:use [medley.core :only [remove-vals]])
  (:require [buddy.core.keys :as keys]
            [buddy.sign.jws :as jws]
            [clj-time.core :as time]))

(defn- build-assertion
  [validity-window-end {:keys [user email given-name family-name common-name]}]
  (let [now (time/now)]
    (remove-vals nil?
                 {:sub         user
                  :exp         (time/plus now (time/seconds validity-window-end))
                  :iat         now
                  :email       email
                  :given_name  given-name
                  :family_name family-name
                  :name        common-name})))

(defn generator
  [{:keys [validity-window-end private-key-path private-key-password alg]}]
  (let [private-key (keys/private-key private-key-path private-key-password)]
    (fn [user]
      (jws/sign (build-assertion validity-window-end user)
                private-key
                {:alg alg}))))

(defn validator
  [{:keys [public-key-path alg]}]
  (let [public-key (keys/public-key public-key-path)]
    (fn [assertion]
      (jws/unsign assertion public-key {:alg alg}))))

(defn user-from-assertion
  [jwt]
  {:user        (:sub jwt)
   :email       (:email jwt)
   :given-name  (:given_name jwt)
   :family-name (:family_name jwt)
   :common-name (:name jwt)})
