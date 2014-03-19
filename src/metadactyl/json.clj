(ns metadactyl.json
  (:use [slingshot.slingshot :only [throw+ try+]])
  (:require [cheshire.core :as cheshire]))

(defn from-json
  "Parses a JSON string, throwing an informative exception if the JSON string
   can't be parsed."
  [str]
  (try+
   (cheshire/decode str true)
   (catch Exception e
     (throw+ {:type   ::invalid_request_body
              :reason "NOT_JSON"
              :detail (.getMessage e)}))))

(defn to-json
  "Converts a Clojure data structure to a JSON string."
  [data]
  (cheshire/encode data))
