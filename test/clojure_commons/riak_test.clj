(ns clojure-commons.riak-test
  (:use [clj-http.fake]
        [clojure.test]
        [clojure-commons.riak])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]))

(defn- list-keys-handler
  "A fake HTTP request handler used in the testing of list-keys."
  [bucket req]
  (is (= {:inputs bucket
          :query  [{:map {:language "javascript"
                          :source   "function(riakObject) {return [riakObject.key];}"}}]}
         (cheshire/decode (slurp (.getContent (:body req))) true)))
  {:status 200
   :body   (cheshire/encode ["foo" "bar" "baz"])})

(deftest list-keys-test
  (with-fake-routes {"http://riak.example.org/mapred" (partial list-keys-handler "bucket")}
    (is (= ["foo" "bar" "baz"] (vec (list-keys "http://riak.example.org" "bucket"))))))
