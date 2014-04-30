(ns riak-migrator.riak
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [cemerick.url :refer [url url-encode]]))

(defn riak-base
  [riak-host riak-port]
  (str "http://" riak-host ":" riak-port))

(defn bucket-url
  [rb]
  (-> (url rb "buckets")
      (assoc :query {:buckets "true"})
      str))

(defn all-keys-url
  ([rb bucket]
   (all-keys-url rb bucket "true"))
  ([rb bucket key-type]
   (-> (url rb "buckets" bucket "keys")
       (assoc :query {:keys key-type})
       str)))

(defn key-url
  [rb bucket key-name]
  (str (url rb "riak" bucket key-name)))

(defn buckets
  "Returns all of the buckets"
  [rb]
  (get-in (http/get (bucket-url rb) {:as :json}) [:body :buckets]))

(defn keys-in-bucket
  [rb bucket]
  (http/get (all-keys-url rb bucket) {:as :json}))

(defn key-value
  [rb bucket key-name]
  (:body (http/get (key-url rb bucket key-name))))
