(ns donkey.util.pgp
  (:require [clojure.java.io :as io]
            [donkey.util.config :as config]
            [mvxcvi.crypto.pgp :as pgp]))

(def ^:private keyring
  (memoize (fn [] (-> (config/keyring-path) io/file pgp/load-secret-keyring))))

(def ^:private get-public-key
  (memoize (fn [] (first (pgp/list-public-keys (keyring))))))

(def ^:private get-private-key
  (memoize
   (fn [id]
     (some-> (keyring)
             (pgp/get-secret-key id)
             (pgp/unlock-key (config/key-password))))))

(defn encrypt
  [s]
  (pgp/encrypt (.getBytes s) (get-public-key)
               :algorithm :aes-256
               :compress  :zip
               :armor     true))

(defn decrypt
  [bs]
  (String. (pgp/decrypt bs get-private-key)))
