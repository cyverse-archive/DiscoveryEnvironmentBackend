(ns data-info.util.transformers
  (:use [medley.core :only [remove-vals]])
  (:import [net.sf.json JSONObject]))


(def remove-nil-vals (partial remove-vals nil?))

(defn object->json-str
  "Converts a Java object to a JSON string."
  [obj]
  (str (JSONObject/fromObject obj)))

(defn object->json-obj
  "Converts a Java object to a JSON object."
  [obj]
  (JSONObject/fromObject obj))
