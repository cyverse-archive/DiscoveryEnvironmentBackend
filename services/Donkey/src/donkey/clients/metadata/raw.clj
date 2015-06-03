(ns donkey.clients.metadata.raw
  (:use [donkey.util.transformers :only [user-params]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [donkey.util.config :as config]))

(defn- metadata-url
  [& components]
  (str (apply curl/url (config/metadata-base) components)))

(defn list-templates
  []
  (http/get (metadata-url "templates")
            {:query-params     (user-params)
             :as               :stream
             :follow_redirects false}))
