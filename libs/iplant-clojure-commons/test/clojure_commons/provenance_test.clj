(ns clojure-commons.provenance-test
  (:use [clojure.test]
        [clojure-commons.provenance]
        [clj-http.fake])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]))

(def ^:private prov-url "http://prov.example.org/")

(defn- echo-request-body
  "Echos the request body back to the caller."
  [req]
  {:status 200
   :body   (slurp (.getContent (:body req)))})

(defn- decode-response
  "Decodes the body of a response."
  [res]
  (cheshire/decode (:body res) true))

(deftest call-prov-log-test
  (with-fake-routes {prov-url echo-request-body}
    (is (= {:foo "bar"} (decode-response (call-prov-log prov-url {:foo "bar"})))
        "call-prov-log should JSON-encode the request body")))

(deftest call-add-object-test
  (with-fake-routes {prov-url echo-request-body}
    (is (= {:foo "bar"} (decode-response (call-add-object prov-url {:foo "bar"})))
        "call-add-object should JSON-encode the request body")))

(deftest register-test
  (with-fake-routes {(str (curl/url prov-url "0.1" "object")) echo-request-body}
    (is (= {:id "foo" :name "bar" :desc "baz"}
           (register prov-url "foo" "bar" "baz"))
        "register should decode the response body for us")))
