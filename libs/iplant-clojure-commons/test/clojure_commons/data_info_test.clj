(ns clojure-commons.data-info-test
  (:use [clojure.test]
        [clojure-commons.data-info])
  (:require [cheshire.core :as cheshire]))

(deftest tree-metaurl-body-test
  (letfn [(get-body [] (cheshire/decode (tree-metaurl-body "http://www.google.com") true))]
    (is (= "tree-urls" (:attr (get-body)))
        "tree-metaurl-body attribute name")
    (is (= "http://www.google.com" (:value (get-body)))
        "tree-metaurl-body attribute value")
    (is (= "" (:unit (get-body)))
        "tree-metaurl-body attribute unit")))
