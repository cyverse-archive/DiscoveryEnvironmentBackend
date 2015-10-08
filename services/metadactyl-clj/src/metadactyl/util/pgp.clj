(ns metadactyl.util.pgp
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clj-pgp.core :as pgp]
            [clj-pgp.keyring :as keyring]
            [clj-pgp.message :as pgp-msg]
            [clojure.java.io :as io]
            [metadactyl.util.config :as config]))

(def ^:private keyring
  (memoize (fn [] (-> (config/pgp-keyring-path) io/file keyring/load-secret-keyring))))

(def ^:private get-public-key
  (memoize (fn [] (first (keyring/list-public-keys (keyring))))))

(def ^:private get-private-key
  (memoize
   (fn []
     (some-> (keyring)
             (keyring/get-secret-key (pgp/hex-id (get-public-key)))
             (pgp/unlock-key (config/pgp-key-password))))))

(defn encrypt
  [s]
  (pgp-msg/encrypt (.getBytes s) (get-public-key)
                   :algorithm :aes-256
                   :compress  :zip))

(defn decrypt
  [bs]
  (if-let [private-key (get-private-key)]
    (String. (pgp-msg/decrypt bs private-key))
    (throw+ {:type  :clojure-commons.exception/invalid-configuration
             :error "unable to unlock the private encryption key"})))
