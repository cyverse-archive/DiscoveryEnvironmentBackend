(ns clojure-commons.osm-test
  (:use [clojure.test]
        [clojure-commons.osm :only [encode]]))

(deftest encode-test
  (is (= "{\"foo\":\"bar\"}" (encode {:foo "bar"}))
      "clojure-commons.osm/encode"))
