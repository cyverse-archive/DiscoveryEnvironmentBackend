(ns metadactyl.clients.data-info
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clojure.string :as string]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]))

(defn- secured-params
  [user]
  {:user (:shortUsername user)})

(defn- data-info-url
  [& components]
  (str (apply curl/url (config/data-info-base-url)
              (map #(string/replace % #"^/+|/+$" "") components))))

(defn get-file-stats
  [user paths]
  (when (seq paths)
    ((comp service/parse-json :body)
     (http/post (data-info-url "stat-gatherer")
                {:query-params (secured-params user)
                 :body         (cheshire/encode {:paths paths})
                 :content-type :json
                 :as           :stream}))))

(defn get-file-contents
  [user path]
  (:body
   (http/get (data-info-url "data" "path" path)
             {:query-params (secured-params user)
              :as           :stream})))

(defn get-path-list-contents
  [user path]
  (->> (slurp (get-file-contents user path))
       (string/split-lines)
       (remove empty?)
       (drop 1)))

(defn create-directory
  [user path]
  (http/post (data-info-url "data" "directories")
             {:query-params (secured-params user)
              :body         (cheshire/encode {:paths [path]})
              :content-type :json
              :as           :stream}))
