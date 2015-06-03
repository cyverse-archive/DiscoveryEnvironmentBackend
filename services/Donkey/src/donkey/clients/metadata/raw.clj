(ns donkey.clients.metadata.raw
  (:use [donkey.util.transformers :only [user-params]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [donkey.util.config :as config]))

(defn- metadata-url
  [& components]
  (str (apply curl/url (config/metadata-base) components)))

(defn list-data-comments
  [target-id]
  (http/get (metadata-url "filesystem" "entry" target-id "comments")
            {:as               :stream
             :follow_redirects false}))

(defn list-app-comments
  [target-id]
  (http/get (metadata-url "apps" target-id "comments")
            {:as               :stream
             :follow_redirects false}))

(defn add-data-comment
  [target-id data-type body]
  (http/post (metadata-url "filesystem" "entry" target-id "comments")
             {:query-params     (user-params {:data-type data-type})
              :body             body
              :content-type     :json
              :as               :stream
              :follow_redirects false}))

(defn add-app-comment
  [target-id body]
  (http/post (metadata-url "apps" target-id "comments")
             {:query-params     (user-params)
              :body             body
              :content-type     :json
              :as               :stream
              :follow_redirects false}))

(defn update-data-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "filesystem" "entry" target-id "comments" comment-id)
    {:query-params     (user-params {:retracted retracted})
     :content-type     :json
     :as               :stream
     :follow_redirects false}))

(defn update-app-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "apps" target-id "comments" comment-id)
    {:query-params     (user-params {:retracted retracted})
     :content-type     :json
     :as               :stream
     :follow_redirects false}))

(defn list-templates
  []
  (http/get (metadata-url "templates")
            {:query-params     (user-params)
             :as               :stream
             :follow_redirects false}))

(defn admin-update-data-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "admin" "filesystem" "entry" target-id "comments" comment-id)
    {:query-params     (user-params {:retracted retracted})
     :content-type     :json
     :as               :stream
     :follow_redirects false}))

(defn admin-update-app-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "admin" "apps" target-id "comments" comment-id)
    {:query-params     (user-params {:retracted retracted})
     :content-type     :json
     :as               :stream
     :follow_redirects false}))

(defn delete-data-comment
  [target-id comment-id]
  (http/delete (metadata-url "admin" "filesystem" "entry" target-id "comments" comment-id)
    {:query-params     (user-params)
     :as               :stream
     :follow_redirects false}))

(defn delete-app-comment
  [target-id comment-id]
  (http/delete (metadata-url "admin" "apps" target-id "comments" comment-id)
    {:query-params     (user-params)
     :as               :stream
     :follow_redirects false}))

(defn get-options
  ([]
     (get-options (user-params)))
  ([params]
     {:query-params     params
      :as               :stream
      :follow-redirects false}))

(defn list-templates
  []
  (http/get (metadata-url "templates") (get-options)))

(defn get-template
  [template-id]
  (http/get (metadata-url "templates" template-id) (get-options)))
