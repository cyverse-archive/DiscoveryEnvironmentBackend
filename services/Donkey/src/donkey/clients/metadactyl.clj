(ns donkey.clients.metadactyl
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [donkey.auth.user-attributes :refer [current-user]]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as service]
            [donkey.util.transformers :as xforms]))

(def metadactyl-sort-params [:limit :offset :sort-field :sort-dir])
(def metadactyl-analysis-listing-params (conj metadactyl-sort-params :include-hidden :filter))
(def metadactyl-search-params (conj metadactyl-sort-params :search))

(defn- secured-params
  ([]
     (secured-params {}))
  ([existing-params]
     (xforms/add-current-user-to-map existing-params))
  ([existing-params param-keys]
     (secured-params (select-keys existing-params param-keys))))

(defn- metadactyl-url
  [& components]
  (str (apply curl/url (config/metadactyl-base) components)))

(defn- add-agave-enabled-flag
  ([]
     (add-agave-enabled-flag {}))
  ([params]
     (assoc params :agave-enabled (str (config/agave-enabled)))))

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

(defn admin-list-tool-requests
  [params]
  (-> (client/get (metadactyl-url "admin" "tool-requests")
                  {:query-params (secured-params (select-keys params (conj metadactyl-sort-params :status)))
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn list-tool-request-status-codes
  [params]
  (-> (client/get (metadactyl-url "tool-requests" "status-codes")
                  {:query-params (secured-params (select-keys params [:filter]))
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn get-tools-in-app
  [app-id]
  (-> (client/get (metadactyl-url "apps" app-id "tools")
                  {:query-params (secured-params)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn get-app-docs
  [app-id]
  (-> (client/get (metadactyl-url "apps" app-id "documentation")
                  {:query-params (secured-params)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn add-app-docs
  [app-id docs]
  (-> (client/post (metadactyl-url "apps" app-id "documentation")
                   {:query-params (secured-params)
                    :content-type :json
                    :body         (cheshire/encode docs)
                    :as           :stream})
      (:body)
      (service/decode-json)))

(defn edit-app-docs
  [app-id docs]
  (-> (client/patch (metadactyl-url "apps" app-id "documentation")
                    {:query-params (secured-params)
                     :content-type :json
                     :body         (cheshire/encode docs)
                     :as           :stream})
      (:body)
      (service/decode-json)))

(defn admin-add-app-docs
  [app-id docs]
  (-> (client/post (metadactyl-url "admin" "apps" app-id "documentation")
                   {:query-params (secured-params)
                    :content-type :json
                    :body         (cheshire/encode docs)
                    :as           :stream})
      (:body)
      (service/decode-json)))

(defn admin-edit-app-docs
  [app-id docs]
  (-> (client/patch (metadactyl-url "admin" "apps" app-id "documentation")
                    {:query-params (secured-params)
                     :content-type :json
                     :body         (cheshire/encode docs)
                     :as           :stream})
      (:body)
      (service/decode-json)))

(defn import-tools
  [body]
  (client/post (metadactyl-url "admin" "tools")
               {:query-params (secured-params)
                :body         (cheshire/encode body)
                :content-type :json}))

(defn update-tool
  [tool-id tool]
  (client/patch (metadactyl-url "admin" "tools" tool-id)
                {:query-params (secured-params)
                 :body         tool
                 :content-type :json}))

(defn get-oauth-access-token
  [api-name {:keys [code state]}]
  (-> (client/get (metadactyl-url "oauth" "access-code" api-name)
                  {:query-params (assoc (secured-params)
                                   :code  code
                                   :state state)})
      (:body)
      (service/decode-json)))
