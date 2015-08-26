(ns data-info.clients.metadata
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [data-info.util.config :as config]))

(defn- metadata-url
  [& components]
  (str (apply curl/url (config/metadata-base-url) components)))

(defn- parse-body
  [response]
  (-> response :body slurp (json/decode true)))

(defn list-metadata-avus
  [target-id]
  (http/get (metadata-url "filesystem" "data" target-id "avus")
    {:as               :stream
     :follow_redirects false}))

(defn get-metadata-avus
  [target-id]
  (parse-body (list-metadata-avus target-id)))
