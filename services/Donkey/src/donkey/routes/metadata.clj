(ns donkey.routes.metadata
  (:use [compojure.core]
        [donkey.services.file-listing]
        [donkey.services.metadata.metadactyl]
        [donkey.util])
  (:require [clojure.tools.logging :as log]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.services.metadata.apps :as apps]
            [donkey.util.config :as config]
            [donkey.util.service :as service]))

(defn app-category-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (GET "/apps/categories" [:as {params :params}]
         (service/success-response (metadactyl/get-app-categories params)))

    (GET "/apps/categories/:category-id" [category-id :as {params :params}]
         (service/success-response (metadactyl/apps-in-category category-id params)))))

(defn admin-category-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/app-routes-enabled))]

    (GET "/apps/categories" [:as {params :params}]
         (get-admin-app-categories params))

    (POST "/apps/categories" [:as req]
          (add-category req))

    (POST "/apps/categories/shredder" [:as req]
          (delete-categories req))

    (DELETE "/apps/categories/:category-id" [category-id]
            (delete-category category-id))

    (PATCH "/apps/categories/:category-id" [category-id :as req]
           (update-category req category-id))))

(defn admin-apps-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/app-routes-enabled))]

    (POST "/apps" [:as req]
          (categorize-apps req))

    (POST "/apps/shredder" [:as req]
          (permanently-delete-apps req))

    (DELETE "/apps/:app-id" [app-id]
            (admin-delete-app app-id))

    (PATCH "/apps/:app-id" [app-id :as req]
           (admin-update-app req app-id))

    (POST "/apps/:app-id/documentation" [app-id :as {:keys [body]}]
          (apps/admin-add-app-docs app-id body))

    (PATCH "/apps/:app-id/documentation" [app-id :as {:keys [body]}]
           (apps/admin-edit-app-docs app-id body))))

(defn apps-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (GET "/apps" [:as {params :params}]
         (service/success-response (metadactyl/search-apps params)))

    (POST "/apps" [:as {:keys [body]}]
          (service/success-response (metadactyl/create-app body)))

    (POST "/apps/arg-preview" [:as {:keys [body]}]
          (service/success-response (metadactyl/preview-args body)))

    (GET "/apps/ids" []
         (service/success-response (metadactyl/list-app-ids)))

    (GET "/apps/elements" [:as {:keys [params]}]
         (get-all-workflow-elements params))

    (GET "/apps/elements/:element-type" [element-type :as {:keys [params]}]
         (get-workflow-elements element-type params))

    (POST "/apps/pipelines" [:as {:keys [body]}]
          (apps/create-pipeline body))

    (PUT "/apps/pipelines/:app-id" [app-id :as {:keys [body]}]
         (apps/update-pipeline app-id body))

    (POST "/apps/pipelines/:app-id/copy" [app-id]
          (apps/copy-workflow app-id))

    (GET "/apps/pipelines/:app-id/ui" [app-id]
         (apps/edit-workflow app-id))

    (POST "/apps/shredder" [:as {:keys [body]}]
          (service/success-response (metadactyl/delete-apps body)))

    (GET "/apps/:app-id" [app-id]
         (service/success-response (metadactyl/get-app app-id)))

    (DELETE "/apps/:app-id" [app-id]
            (service/success-response (metadactyl/delete-app app-id)))

    (PATCH "/apps/:app-id" [app-id :as {:keys [body]}]
           (service/success-response (metadactyl/relabel-app app-id body)))

    (PUT "/apps/:app-id" [app-id :as req]
         (update-app req app-id))

    (POST "/apps/:app-id/copy" [app-id :as req]
          (copy-app req app-id))

    (GET "/apps/:app-id/details" [app-id]
         (apps/get-app-details app-id))

    (GET "/apps/:app-id/documentation" [app-id]
         (apps/get-app-docs app-id))

    (POST "/apps/:app-id/documentation" [app-id :as {:keys [body]}]
          (apps/add-app-docs app-id body))

    (PATCH "/apps/:app-id/documentation" [app-id :as {:keys [body]}]
           (apps/edit-app-docs app-id body))

    (DELETE "/apps/:app-id/favorite" [app-id]
            (apps/remove-favorite-app app-id))

    (PUT "/apps/:app-id/favorite" [app-id]
         (apps/add-favorite-app app-id))

    (GET "/apps/:app-id/is-publishable" [app-id]
         (app-publishable? app-id))

    (POST "/apps/:app-id/publish" [app-id :as req]
          (make-app-public req app-id))

    (DELETE "/apps/:app-id/rating" [app-id]
            (apps/delete-rating app-id))

    (POST "/apps/:app-id/rating" [app-id :as {body :body}]
          (apps/rate-app body app-id))

    (GET "/apps/:app-id/tasks" [app-id]
         (apps/list-app-tasks app-id))

    (GET "/apps/:app-id/tools" [app-id]
         (apps/get-tools-in-app app-id))

    (GET "/apps/:app-id/ui" [app-id]
         (edit-app app-id))))

(defn analysis-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/analyses" [:as {:keys [params]}]
        (apps/list-jobs params))

   (POST "/analyses" [:as {:keys [body]}]
         (apps/submit-job body))

   (PATCH "/analyses/:analysis-id" [analysis-id :as {body :body}]
          (apps/update-job analysis-id body))

   (DELETE "/analyses/:analysis-id" [analysis-id]
           (apps/delete-job analysis-id))

   (POST "/analyses/shredder" [:as {:keys [body]}]
         (apps/delete-jobs body))

   (GET "/analyses/:analysis-id/parameters" [analysis-id]
        (apps/get-parameter-values analysis-id))

   (GET "/analyses/:analysis-id/relaunch-info" [analysis-id]
        (apps/get-app-rerun-info analysis-id))

   (POST "/analyses/:uuid/stop" [uuid]
         (apps/stop-job uuid))))

(defn admin-reference-genomes-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/app-routes-enabled))]

    (POST "/reference-genomes" [:as req]
          (add-reference-genome req))

    (PUT "/reference-genomes" [:as req]
         (replace-reference-genomes req))

    (DELETE "/reference-genomes/:reference-genome-id" [reference-genome-id]
            (delete-reference-genomes reference-genome-id))

    (PATCH "/reference-genomes/:reference-genome-id" [reference-genome-id :as req]
           (update-reference-genome req reference-genome-id))))

(defn reference-genomes-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (GET "/reference-genomes" [:as {params :params}]
         (list-reference-genomes params))

    (GET "/reference-genomes/:reference-genome-id" [reference-genome-id]
         (get-reference-genome reference-genome-id))))

(defn admin-tool-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
        (config/app-routes-enabled))]

    (POST "/tools" [:as {:keys [body]}]
          (import-tools body))

    (GET "/tool-requests" [:as {params :params}]
         (admin-list-tool-requests params))

    (GET "/tool-requests/:request-id" [request-id]
         (get-tool-request request-id))

    (POST "/tool-requests/:request-id/status" [request-id :as req]
          (update-tool-request req request-id))))

(defn tool-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (GET "/tools" [:as req]
         (search-tools req))

    (GET "/tools/:tool-id" [tool-id :as req]
         (get-tool req tool-id))

    (GET "/tool-requests" []
         (list-tool-requests))

    (POST "/tool-requests" [:as req]
          (submit-tool-request req))

    (GET "/tool-requests/status-codes" [:as {params :params}]
         (list-tool-request-status-codes params))))

(defn secured-metadata-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/bootstrap" [:as req]
        (bootstrap req))

   (GET "/logout" [:as {params :params}]
        (logout params))

   (GET "/default-output-dir" []
        (get-default-output-dir))

   (POST "/default-output-dir" [:as {body :body}]
         (reset-default-output-dir body))

   (PUT "/feedback" [:as {body :body}]
        (provide-user-feedback body))))
