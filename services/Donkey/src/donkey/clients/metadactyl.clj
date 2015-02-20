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

(defn- secured-params
  ([]
     (secured-params {}))
  ([existing-params]
     (xforms/add-current-user-to-map existing-params)))

(defn- metadactyl-url
  [& components]
  (str (apply curl/url (config/metadactyl-base-url) components)))

(defn- add-agave-enabled-flag
  ([]
     (add-agave-enabled-flag {}))
  ([params]
     (assoc params :agave-enabled (str (config/agave-enabled)))))

(defn get-app-categories
  [params]
  (-> (client/get (metadactyl-url "apps" "categories")
                  {:query-params (secured-params (select-keys params [:public]))
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn- apps-in-real-category
  [category-id params]
  (-> (client/get (metadactyl-url "apps" "categories" category-id)
                  {:query-params (secured-params (select-keys params metadactyl-sort-params))
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn- virtual-category-params
  [category-id]
  (condp = category-id
    "my-public-apps" {:integrator_email (:email current-user)
                      :is_public        true}
    (service/bad-request (str "unrecognized virtual app category: " category-id))))

(defn- add-virtual-category-params
  [params category-id]
  (merge (secured-params params) (virtual-category-params category-id)))

(defn- apps-in-virtual-category
  [category-id params]
  (-> (client/get (metadactyl-url "apps")
                  {:query-params (add-virtual-category-params params category-id)})))

(defn apps-in-category
  [category-id & [params]]
  (if (util/is-uuid? category-id)
    (apps-in-real-category category-id params)
    (apps-in-virtual-category category-id params)))

(defn search-apps
  [search-term]
  (-> (client/get (metadactyl-url "apps")
                  {:query-params (secured-params {:search search-term})
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn get-app
  [app-id]
  (-> (client/get (metadactyl-url "apps" app-id)
                  {:query-params (secured-params)
                   :as           :stream})
      (:body)
      (service/decode-json)))

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

(defn app-publishable?
  [app-id]
  (-> (client/get (metadactyl-url "apps" app-id "is-publishable")
                  {:query-params (secured-params)
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

(defn get-app-details
  [app-id]
  (-> (client/get (metadactyl-url "apps" app-id "details")
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

(defn list-app-tasks
  [app-id]
  (-> (client/get (metadactyl-url "apps" app-id "tasks")
                  {:query-params (secured-params)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn edit-workflow
  [app-id]
  (-> (client/get (metadactyl-url "apps" "pipelines" app-id "ui")
                  {:query-params (secured-params)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn copy-workflow
  [app-id]
  (-> (client/post (metadactyl-url "apps" "pipelines" app-id "copy")
                   {:query-params (secured-params)
                    :as           :stream})
      (:body)
      (service/decode-json)))

(defn create-pipeline
  [pipeline]
  (-> (client/post (metadactyl-url "apps" "pipelines")
                   {:query-params (secured-params)
                    :content-type :json
                    :body         (cheshire/encode pipeline)
                    :as           :stream})
      (:body)
      (service/decode-json)))

(defn update-pipeline
  [app-id pipeline]
  (-> (client/put (metadactyl-url "apps" "pipelines" app-id)
                  {:query-params (secured-params)
                   :content-type :json
                   :body         (cheshire/encode pipeline)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn submit-job
  [submission]
  (-> (client/post (metadactyl-url "analyses")
                   {:query-params (secured-params)
                    :content-type :json
                    :body         (cheshire/encode submission)
                    :as           :stream})
      (:body)
      (service/decode-json)))

(defn add-favorite-app
  [app-id]
  (-> (client/put (metadactyl-url "apps" app-id "favorite")
                  {:query-params (secured-params)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn remove-favorite-app
  [app-id]
  (-> (client/delete (metadactyl-url "apps" app-id "favorite")
                     {:query-params (secured-params)
                      :as           :stream})
      (:body)
      (service/decode-json)))

(defn- rate-app-request
  [rating comment-id]
  (xforms/remove-nil-vals
   {:rating      rating
    :comment_id  comment-id}))

(defn rate-app
  [app-id rating comment-id]
  (-> (client/post (metadactyl-url "apps" app-id "rating")
                   {:query-params (secured-params)
                    :body         (cheshire/encode (rate-app-request rating comment-id))
                    :content-type :json
                    :as           :stream})
      (:body)
      (service/decode-json)))

(defn delete-rating
  [app-id]
  (-> (client/delete (metadactyl-url "apps" app-id "rating")
                     {:query-params (secured-params)
                      :as           :stream})
      (:body)
      (service/decode-json)))

(defn import-tools
  [body]
  (client/post (metadactyl-url "admin" "tools")
               {:query-params (secured-params)
                :body         (cheshire/encode body)
                :content-type :json}))

(defn get-oauth-access-token
  [api-name {:keys [code state]}]
  (client/get (metadactyl-url "oauth" "access-code" api-name)
              {:query-params (assoc (secured-params)
                               :code  code
                               :state state)}))
