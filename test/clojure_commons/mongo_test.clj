(ns clojure-commons.mongo-test
  (:use [clojure.test]
        [clojure-commons.mongo]))

(deftest obj->map-test
  (is (= {:foo "bar" :baz "quux"}
         (obj->map "{\"foo\":\"bar\",\"baz\":\"quux\"}"))
      "obj->map"))
