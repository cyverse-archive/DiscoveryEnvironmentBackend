(ns riak-migrator.riak
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url url-encode]]))

(defn riak-base
  [riak-host riak-port]
  (str "http://" riak-host ":" riak-port))

(defn- bucket-url
  [rb]
  (-> (url rb "buckets")
      (assoc :query {:buckets "true"})
      str))

(defn- all-keys-url
  [rb bucket]
  (-> (url rb "buckets" bucket "keys")
      (assoc :query {:keys "true"})
      str))

(defn- key-url
  [rb bucket key-name]
  (str (url rb "riak" bucket key-name)))

(defn buckets
  "Returns all of the buckets"
  [rb]
  (get-in (http/get (bucket-url rb) {:as :json}) [:body :buckets]))

(defn keys-in-bucket
  [rb bucket]
  (get-in (http/get (all-keys-url rb bucket) {:as :json}) [:body :keys]))

(defn key-value
  [rb bucket key-name]
  (get-in (http/get (key-url rb bucket key-name)) [:body]))
