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
  (str (apply curl/url (config/metadactyl-unprotected-base-url) components)))

(defn- unsecured-url
  [& components]
  (str (apply curl/url (config/metadactyl-unprotected-base-url) components)))

(defn- secured-url
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
  (-> (client/get (metadactyl-url "apps" app-id "ui")
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

(defn get-deployed-components-in-app
  [app-id]
  (-> (client/get (secured-url "get-components-in-analysis" app-id)
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

(defn list-app-data-objects
  [app-id]
  (-> (client/get (metadactyl-url "apps" app-id "data-objects")
                  {:query-params (secured-params)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn edit-workflow
  [app-id]
  (-> (client/get (metadactyl-url "apps" app-id "pipeline-ui")
                  {:query-params (secured-params)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn copy-workflow
  [app-id]
  (-> (client/get (metadactyl-url "apps" app-id "copy-pipeline")
                  {:query-params (secured-params)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn submit-job
  [workspace-id submission]
  (-> (client/put (secured-url "workspaces" workspace-id "newexperiment")
                  {:query-params (secured-params)
                   :content-type :json
                   :body         (cheshire/encode submission)
                   :as           :stream})
      (:body)
      (service/decode-json)))

(defn- update-favorites-request
  [app-id favorite?]
  {:analysis_id   app-id
   :user_favorite favorite?})

(defn update-favorites
  [app-id favorite?]
  (-> (client/post (secured-url "update-favorites")
                   {:query-params (secured-params)
                    :body         (cheshire/encode (update-favorites-request app-id favorite?))
                    :as           :stream})
      (:body)
      (service/decode-json)))

(defn- rate-app-request
  [app-id rating comment-id]
  {:analysis_id app-id
   :rating      rating
   :comment_id  comment-id})

(defn rate-app
  [app-id rating comment-id]
  (-> (client/post (secured-url "rate-analysis")
                   {:query-params (secured-params)
                    :body         (cheshire/encode (rate-app-request app-id rating comment-id))
                    :as           :stream})
      (:body)
      (service/decode-json)))

(defn delete-rating
  [app-id]
  (-> (client/post (secured-url "delete-rating")
                   {:query-params (secured-params)
                    :body         (cheshire/encode {:analysis_id app-id})
                    :as           :stream})
      (:body)
      (service/decode-json)))
