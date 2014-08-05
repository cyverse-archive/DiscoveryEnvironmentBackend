(ns donkey.clients.jex
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [donkey.util.config :as cfg]))

(defn- jex-url
  [& components]
  (str (apply curl/url (cfg/jex-base-url) components)))

(defn stop-job
  [uuid]
  (http/delete (jex-url "stop" uuid)
               {:throw-exceptions false
                :as               :json}))
