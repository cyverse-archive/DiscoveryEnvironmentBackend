(ns donkey.clients.metadata.raw
  (:use [donkey.util.transformers :only [user-params]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [donkey.util.config :as config]))

(defn- metadata-url
  [& components]
  (str (apply curl/url (config/metadata-base) components)))

(defn get-options
  ([]
     (get-options {}))
  ([params]
     {:query-params     (user-params params)
      :as               :stream
      :follow-redirects false}))

(def delete-options get-options)

(defn post-options
  ([body]
     (post-options body {}))
  ([body params]
     {:query-params     (user-params params)
      :body             body
      :content-type     :json
      :as               :stream
      :follow-redirects false}))

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
             (post-options body {:data-type data-type})))

(defn add-app-comment
  [target-id body]
  (http/post (metadata-url "apps" target-id "comments") (post-options body)))

(defn update-data-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "filesystem" "entry" target-id "comments" comment-id)
              (post-options nil {:retracted retracted})))

(defn update-app-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "apps" target-id "comments" comment-id)
              (post-options nil {:retracted retracted})))

(defn list-favorites
  [entity-type]
  (http/get (metadata-url "favorites" "filesystem") (get-options {:entity-type entity-type})))

(defn remove-favorite
  [target-id]
  (http/delete (metadata-url "favorites" "filesystem" target-id) (delete-options)))

(defn add-favorite
  [target-id data-type]
  (http/put (metadata-url "favorites" "filesystem" target-id)
            (post-options nil {:data-type data-type})))

(defn filter-favorites
  [uuids]
  (http/post (metadata-url "favorites" "filter") (post-options (json/encode {:filesystem uuids}))))

(defn admin-update-data-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "admin" "filesystem" "entry" target-id "comments" comment-id)
              (post-options nil {:retracted retracted})))

(defn admin-update-app-retract-status
  [target-id comment-id retracted]
  (http/patch (metadata-url "admin" "apps" target-id "comments" comment-id)
              (post-options nil {:retracted retracted})))

(defn delete-data-comment
  [target-id comment-id]
  (http/delete (metadata-url "admin" "filesystem" "entry" target-id "comments" comment-id)
               (delete-options)))

(defn delete-app-comment
  [target-id comment-id]
  (http/delete (metadata-url "admin" "apps" target-id "comments" comment-id) (delete-options)))

(defn list-templates
  []
  (http/get (metadata-url "templates") (get-options)))

(defn get-template
  [template-id]
  (http/get (metadata-url "templates" template-id) (get-options)))

(defn get-attribute
  [attr-id]
  (http/get (metadata-url "templates" "attr" attr-id) (get-options)))

(defn admin-list-templates
  []
  (http/get (metadata-url "admin" "templates") (get-options)))

(defn admin-add-template
  [user-id template]
  (http/post (metadata-url "admin" "templates") (post-options template {:user-id user-id})))
