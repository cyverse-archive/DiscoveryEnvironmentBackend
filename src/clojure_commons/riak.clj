(ns clojure-commons.riak
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-http.client :as client]))

(defn- mapreduce-url
  "Generates a URL that can be used for Riak MapReduce jobs."
  [base]
  (str (curl/url base "mapred")))

(defn- obj-url
  "Generates a URL that refers to an object in Riak."
  [base bucket key]
  (str (curl/url base "riak" bucket key)))

(defn list-keys
  "Lists the keys in a Riak bucket.  This can take a while.

   Parameters:
       base   - the base URL used to connect to Riak.
       bucket - the Riak bucket to list the keys in.
       opts   - the options to pass to the HTTP client library.

   Returns a sequence of keys."
  ([base bucket]
     (list-keys base bucket {}))
  ([base bucket opts]
     (let [body {:inputs bucket
                 :query  [{:map
                           {:language :javascript
                            :source   "function(riakObject) {return [riakObject.key];}"}}]}]
       (cheshire/decode
        (:body (client/post (mapreduce-url base)
                            (merge opts {:body         (cheshire/encode body)
                                         :content-type :json})))
        true))))

(defn insert-object
  "Inserts an object into a Riak bucket.

   Parameters:
       base   - the base URL used to connect to Riak.
       bucket - the Riak bucket name.
       key    - the key used to reference the object.
       obj    - the object itself.
       opts   - the options to pass to the HTTP client library."
  ([base bucket key obj]
     (insert-object base bucket key obj {}))
  ([base bucket key obj opts]
     (client/post (obj-url base bucket key)
                  (merge opts {:body obj}))))

(defn get-object
  "Retrieves an object from a Riak bucket.

   Parameters:
       base   - the base URL used to connect to Riak.
       bucket - the Riak bucket name.
       key    - the key used to reference the object.
       opts   - the options to pass to the HTTP client library."
  ([base bucket key]
     (get-object base bucket key {}))
  ([base bucket key opts]
     (client/get (obj-url base bucket key) opts)))

(defn delete-object
  "Deletes an object in a Riak bucket.

   Parameters:
       base   - the base URL used to connect to Riak.
       bucket - the Riak bucket name.
       key    - the key used to reference the object.
       opts   - the options to pass to the HTTP client library."
  ([base bucket key]
     (delete-object base bucket key {}))
  ([base bucket key opts]
     (client/delete (obj-url base bucket key) opts)))
