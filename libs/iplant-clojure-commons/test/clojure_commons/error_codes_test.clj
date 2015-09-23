(ns clojure-commons.error-codes-test
  (:use [clojure.test]
        [clojure-commons.error-codes])
  (:require [cheshire.core :as cheshire]))

(def ^:private err-obj {:foo "bar"})

(deftest err-resp-test
  (is (= 500 (:status (err-resp err-obj)))
      "err-resp status with one argument")
  (is (= err-obj (cheshire/decode (:body (err-resp err-obj)) true))
      "err-resp body with one argument")
  (is (= 500 (:status (err-resp "baz" err-obj)))
      "err-resp status with two arguments")
  (is (= err-obj (cheshire/decode (:body (err-resp "baz" err-obj)) true))
      "err-resp body with two arguments"))

(defn complete-success-resp
  [status]
  {:status status
   :body   "foo"})

(defn expected-success-resp
  [action s]
  {:status 200
   :body   (cond (map? s)          (cheshire/encode s)
                 (not (string? s)) (str s)
                 :else             s)})

(deftest success-resp-test
  (is (= (complete-success-resp 200) (success-resp "baz" (complete-success-resp 200)))
      "success-resp complete response with 200 status")
  (is (= (complete-success-resp 404) (success-resp "baz" (complete-success-resp 404)))
      "success-resp complete response with 404 status")
  (is (= (expected-success-resp "baz" {:foo "bar"})
         (success-resp "baz" {:foo "bar"}))
      "success-resp partial response with map")
  (is (= (expected-success-resp "baz" ["foo" "bar"])
         (success-resp "baz" ["foo" "bar"]))
      "success-resp partial response with vector")
  (is (= (expected-success-resp "baz" "blarg")
         (success-resp "baz" "blarg"))
      "succes-resp partial response with string"))
