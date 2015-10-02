(ns clojure-commons.jwt
  (:use [clojure.java.io :only [file]]
        [medley.core :only [remove-vals]]
        [slingshot.slingshot :only [try+]])
  (:require [buddy.core.keys :as keys]
            [buddy.sign.jws :as jws]
            [clj-time.core :as time]
            [clojure.string :as string]
            [clojure-commons.exception-util :as cx-util]))

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

(defn- list-keys
  [accepted-keys-dir]
  (when-not (string/blank? accepted-keys-dir)
    (map str (filter #(.isFile %) (.listFiles (file accepted-keys-dir))))))

(defn- load-public-keys
  [public-key-path accepted-keys-dir]
  (for [path (cons public-key-path (list-keys accepted-keys-dir))]
    (keys/public-key path)))

(defn- check-key
  [key alg assertion]
  (try+
   (jws/unsign assertion key {:alg alg})
   (catch [:cause :signature] _ nil)))

(defn- unsign-assertion
  [accepted-keys alg assertion]
  (or (first (remove nil? (map #(check-key % alg assertion) accepted-keys)))
      (throw (ex-info "Untrusted JWT signature."
                      {:type :validation :cause :signature}))))

(defn validator
  [{:keys [public-key-path accepted-keys-dir alg]}]
  (let [accepted-keys (load-public-keys public-key-path accepted-keys-dir)]
    (partial unsign-assertion accepted-keys alg)))

(defn user-from-assertion
  [jwt]
  {:user        (:sub jwt)
   :email       (:email jwt)
   :given-name  (:given_name jwt)
   :family-name (:family_name jwt)
   :common-name (:name jwt)})
