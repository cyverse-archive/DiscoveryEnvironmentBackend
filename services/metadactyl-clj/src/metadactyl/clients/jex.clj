(ns metadactyl.clients.jex
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [metadactyl.util.config :as cfg]))

(defn- jex-url
  [& components]
  (str (apply curl/url (cfg/jex-base-url) components)))

(defn stop-job
  [uuid]
  (http/delete (jex-url "stop" uuid)
               {:throw-exceptions false
                :as               :json}))

(defn submit-job
  [job]
  (http/post (jex-url)
             {:body         (cheshire/encode job)
              :content-type :json}))
