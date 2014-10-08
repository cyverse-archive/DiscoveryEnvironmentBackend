(ns data-info.util.transformers
  (:use [medley.core :only [remove-vals]])
  (:require [cheshire.core :as json]
            [slingshot.slingshot :refer [try+ throw+]]
            [clojure-commons.error-codes :as error])
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


(defn parse-body
  [body]
  (try+
    (json/parse-string body true)
    (catch Exception e
      (throw+ {:error_code error/ERR_INVALID_JSON :message (str e)}))))
