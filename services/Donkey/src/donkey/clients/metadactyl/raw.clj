(ns donkey.clients.metadactyl.raw
  (:use [donkey.util.transformers :only [secured-params]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [donkey.util.config :as config]))

(def metadactyl-sort-params [:limit :offset :sort-field :sort-dir])
(def metadactyl-analysis-listing-params (conj metadactyl-sort-params :include-hidden :filter))
(def metadactyl-search-params (conj metadactyl-sort-params :search))

(defn- metadactyl-url
  [& components]
  (str (apply curl/url (config/metadactyl-base) components)))

(defn get-all-workflow-elements
  [params]
  (client/get (metadactyl-url "apps" "elements")
              {:query-params     (secured-params params [:include-hidden])
               :as               :stream
               :follow-redirects false}))

(defn get-workflow-elements
  [element-type params]
  (client/get (metadactyl-url "apps" "elements" element-type)
              {:query-params     (secured-params params [:include-hidden])
               :as               :stream
               :follow-redirects false}))

(defn get-app-categories
  [params]
  (client/get (metadactyl-url "apps" "categories")
              {:query-params     (secured-params params [:public])
               :as               :stream
               :follow-redirects false}))

(defn apps-in-category
  [category-id params]
  (client/get (metadactyl-url "apps" "categories" category-id)
              {:query-params     (secured-params params metadactyl-sort-params)
               :as               :stream
               :follow-redirects false}))

(defn search-apps
  [params]
  (client/get (metadactyl-url "apps")
              {:query-params     (secured-params params metadactyl-search-params)
               :as               :stream
               :follow-redirects false}))

(defn create-app
  [app]
  (client/post (metadactyl-url "apps")
               {:query-params     (secured-params)
                :body             app
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn preview-args
  [app]
  (client/post (metadactyl-url "apps" "arg-preview")
               {:query-params     (secured-params)
                :body             app
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn list-app-ids
  []
  (client/get (metadactyl-url "apps" "ids")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn delete-apps
  [deletion-request]
  (client/post (metadactyl-url "apps" "shredder")
               {:query-params     (secured-params)
                :body             deletion-request
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn get-app
  [app-id]
  (client/get (metadactyl-url "apps" app-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn delete-app
  [app-id]
  (client/delete (metadactyl-url "apps" app-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn relabel-app
  [app-id relabel-request]
  (client/patch (metadactyl-url "apps" app-id)
                {:query-params     (secured-params)
                 :body             relabel-request
                 :content-type     :json
                 :as               :stream
                 :follow-redirects false}))

(defn update-app
  [app-id update-request]
  (client/put (metadactyl-url "apps" app-id)
              {:query-params     (secured-params)
               :body             update-request
               :content-type     :json
               :as               :stream
               :follow-redirects false}))

(defn copy-app
  [app-id]
  (client/post (metadactyl-url "apps" app-id "copy")
               {:query-params     (secured-params)
                :as               :stream
                :follow-redirects false}))

(defn get-app-details
  [app-id]
  (client/get (metadactyl-url "apps" app-id "details")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn remove-favorite-app
  [app-id]
  (client/delete (metadactyl-url "apps" app-id "favorite")
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn add-favorite-app
  [app-id]
  (client/put (metadactyl-url "apps" app-id "favorite")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn app-publishable?
  [app-id]
  (client/get (metadactyl-url "apps" app-id "is-publishable")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn make-app-public
  [app-id app]
  (client/post (metadactyl-url "apps" app-id "publish")
               {:query-params     (secured-params)
                :body             app
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn delete-rating
  [app-id]
  (client/delete (metadactyl-url "apps" app-id "rating")
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn rate-app
  [app-id rating]
  (client/post (metadactyl-url "apps" app-id "rating")
               {:query-params     (secured-params)
                :body             rating
                :content-type     :json
                :as               :stream
                :follow-redirects false}))

(defn list-app-tasks
  [app-id]
  (client/get (metadactyl-url "apps" app-id "tasks")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-app-ui
  [app-id]
  (client/get (metadactyl-url "apps" app-id "ui")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn add-pipeline
  [pipeline]
  (client/post (metadactyl-url "apps" "pipelines")
               {:query-params     (secured-params)
                :content-type     :json
                :body             pipeline
                :as               :stream
                :follow-redirects false}))

(defn update-pipeline
  [app-id pipeline]
  (client/put (metadactyl-url "apps" "pipelines" app-id)
              {:query-params     (secured-params)
               :content-type     :json
               :body             pipeline
               :as               :stream
               :follow-redirects false}))

(defn copy-pipeline
  [app-id]
  (client/post (metadactyl-url "apps" "pipelines" app-id "copy")
               {:query-params     (secured-params)
                :as               :stream
                :follow-redirects false}))

(defn edit-pipeline
  [app-id]
  (client/get (metadactyl-url "apps" "pipelines" app-id "ui")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn list-jobs
  [params]
  (client/get (metadactyl-url "analyses")
              {:query-params     (secured-params params metadactyl-analysis-listing-params)
               :as               :stream
               :follow-redirects false}))

(defn submit-job
  [submission]
  (client/post (metadactyl-url "analyses")
               {:query-params     (secured-params)
                :content-type     :json
                :body             submission
                :as               :stream
                :follow-redirects false}))

(defn update-job
  [analysis-id body]
  (client/patch (metadactyl-url "analyses" analysis-id)
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             body
                 :as               :stream
                 :follow-redirects false}))

(defn delete-job
  [analysis-id]
  (client/delete (metadactyl-url "analyses" analysis-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn delete-jobs
  [body]
  (client/post (metadactyl-url "analyses" "shredder")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn get-job-params
  [analysis-id]
  (client/get (metadactyl-url "analyses" analysis-id "parameters")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-job-relaunch-info
  [analysis-id]
  (client/get (metadactyl-url "analyses" analysis-id "relaunch-info")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn list-job-steps
  [analysis-id]
  (client/get (metadactyl-url "analyses" analysis-id "steps")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn stop-job
  [analysis-id]
  (client/post (metadactyl-url "analyses" analysis-id "stop")
               {:query-params     (secured-params)
                :as               :stream
                :follow-redirects false}))

(defn categorize-apps
  [body]
  (client/post (metadactyl-url "admin" "apps")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn permanently-delete-apps
  [body]
  (client/post (metadactyl-url "admin" "apps" "shredder")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn admin-delete-app
  [app-id]
  (client/delete (metadactyl-url "admin" "apps" app-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn admin-update-app
  [app-id body]
  (client/patch (metadactyl-url "admin" "apps" app-id)
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             body
                 :as               :stream
                 :follow-redirects false}))

(defn get-admin-app-categories
  [params]
  (client/get (metadactyl-url "admin" "apps" "categories")
              {:query-params     (secured-params params metadactyl-sort-params)
               :as               :stream
               :follow-redirects false}))

(defn add-category
  [body]
  (client/post (metadactyl-url "admin" "apps" "categories")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn delete-categories
  [body]
  (client/post (metadactyl-url "admin" "apps" "categories" "shredder")
               {:query-params     (secured-params)
                :content-type     :json
                :body             body
                :as               :stream
                :follow-redirects false}))

(defn delete-category
  [category-id]
  (client/delete (metadactyl-url "admin" "apps" "categories" category-id)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn update-category
  [category-id body]
  (client/patch (metadactyl-url "admin" "apps" "categories" category-id)
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             body
                 :as               :stream
                 :follow-redirects false}))

(defn get-app-docs
  [app-id]
  (client/get (metadactyl-url "apps" app-id "documentation")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn edit-app-docs
  [app-id docs]
  (client/patch (metadactyl-url "apps" app-id "documentation")
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             docs
                 :as               :stream
                 :follow-redirects false}))

(defn add-app-docs
  [app-id docs]
  (client/post (metadactyl-url "apps" app-id "documentation")
               {:query-params     (secured-params)
                :content-type     :json
                :body             docs
                :as               :stream
                :follow-redirects false}))

(defn admin-edit-app-docs
  [app-id docs]
  (client/patch (metadactyl-url "admin" "apps" app-id "documentation")
                {:query-params     (secured-params)
                 :content-type     :json
                 :body             docs
                 :as               :stream
                 :follow-redirects false}))

(defn admin-add-app-docs
  [app-id docs]
  (client/post (metadactyl-url "admin" "apps" app-id "documentation")
               {:query-params     (secured-params)
                :content-type     :json
                :body             docs
                :as               :stream
                :follow-redirects false}))


(defn get-oauth-access-token
  [api-name params]
  (client/get (metadactyl-url "oauth" "access-code" api-name)
              {:query-params     (secured-params params [:code :state])
               :as               :stream
               :follow-redirects false}))

(defn admin-list-tool-requests
  [params]
  (client/get (metadactyl-url "admin" "tool-requests")
              {:query-params     (secured-params params (conj metadactyl-sort-params :status))
               :as               :stream
               :follow-redirects false}))

(defn list-tool-request-status-codes
  [params]
  (client/get (metadactyl-url "tool-requests" "status-codes")
              {:query-params     (secured-params params [:filter])
               :as               :stream
               :follow-redirects false}))

(defn get-tools-in-app
  [app-id]
  (client/get (metadactyl-url "apps" app-id "tools")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn import-tools
  [body]
  (client/post (metadactyl-url "admin" "tools")
               {:query-params     (secured-params)
                :as               :stream
                :body             body
                :content-type     :json
                :follow-redirects false}))

(defn update-tool
  [tool-id tool]
  (client/patch (metadactyl-url "admin" "tools" tool-id)
                {:query-params     (secured-params)
                 :as               :stream
                 :body             tool
                 :content-type     :json
                 :follow-redirects false}))

(defn search-tools
  [params]
  (client/get (metadactyl-url "tools")
              {:query-params     (secured-params params [:search :include-hidden])
               :as               :stream
               :follow-redirects :false}))

(defn get-tool
  [tool-id]
  (client/get (metadactyl-url "tools" tool-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn list-reference-genomes
  [params]
  (client/get (metadactyl-url "reference-genomes")
              {:query-params     (secured-params params [:deleted])
               :as               :stream
               :follow-redirects false}))

(defn get-reference-genome
  [reference-genome-id]
  (client/get (metadactyl-url "reference-genomes" reference-genome-id)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-workspace
  []
  (client/get (metadactyl-url "workspaces")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-collaborators
  []
  (client/get (metadactyl-url "collaborators")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn add-collaborators
  [body]
  (client/post (metadactyl-url "collaborators")
               {:query-params     (secured-params)
                :as               :stream
                :body             body
                :content-type     :json
                :follow-redirects false}))

(defn remove-collaborators
  [body]
  (client/post (metadactyl-url "collaborators" "shredder")
               {:query-params     (secured-params)
                :as               :stream
                :body             body
                :content-type     :json
                :follow-redirects false}))

(defn get-users-by-id
  [body]
  (client/post (metadactyl-url "users" "by-id")
               {:query-params     (secured-params)
                :as               :stream
                :body             body
                :content-type     :json
                :follow-redirects false}))

(defn get-authenticated-user
  []
  (client/get (metadactyl-url "users" "authenticated")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn record-login
  [ip-address user-agent]
  (let [params {:ip-address ip-address :user-agent user-agent}]
    (client/post (metadactyl-url "users" "login")
                 {:query-params     (secured-params params)
                  :as               :stream
                  :follow-redirects false})))

(defn record-logout
  [ip-address login-time]
  (let [params {:ip-address ip-address :login-time login-time}]
    (client/post (metadactyl-url "users" "logout")
                 {:query-params     (secured-params params)
                  :as               :stream
                  :follow-redirects false})))
