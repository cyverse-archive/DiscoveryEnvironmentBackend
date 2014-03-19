(ns metadactyl.transformers
  (:use [clojure.java.io :only [reader]])
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log])
  (:import [net.sf.json JSONObject]))

(defn object->json-str
  "Converts a Java object to a JSON string."
  [obj]
  (.toString (JSONObject/fromObject obj)))

(defn object->json-obj
  "Converts a Java object to a JSON object."
  [obj]
  (JSONObject/fromObject obj))

(defn add-username-to-json
  "Adds the name of the currently authenticated user to a JSON object in the
   body of a request, and returns only the updated body."
  [req]
  (let [m        (cheshire/decode-stream (reader (:body req)) true)
        username (get-in req [:user-attributes "uid"])]
    (cheshire/encode (assoc m :user username))))

(defn add-workspace-id
  "Adds a workspace ID to a JSON request body."
  [body workspace-id]
  (cheshire/encode (assoc (cheshire/decode body true) :workspace_id workspace-id)))

(defn string->long
  "Converts a String to a long."
  [string]
  (try
    (Long/parseLong string)
    (catch NumberFormatException e
      (throw (IllegalArgumentException. e)))))
