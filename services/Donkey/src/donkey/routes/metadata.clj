(ns donkey.routes.metadata
  (:use [compojure.core]
        [donkey.services.file-listing]
        [donkey.services.metadata.metadactyl]
        [donkey.util.service]
        [donkey.util])
  (:require [donkey.util.config :as config]
            [donkey.services.metadata.apps :as apps]))

(defn app-category-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (GET "/apps/categories" [:as {params :params}]
         (apps/get-app-categories params))

    (GET "/apps/categories/:app-group-id" [app-group-id :as {params :params}]
         (apps/apps-in-category app-group-id params))))

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
           (admin-update-app req app-id))))

(defn apps-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (GET "/apps" [:as {params :params}]
         (apps/search-apps params))

    (POST "/apps" [:as req]
          (create-app req))

    (POST "/apps/arg-preview" [:as req]
          (preview-args req))

    (GET "/apps/ids" []
         (get-all-app-ids))

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

    (POST "/apps/shredder" [:as req]
          (delete-apps req))

    (GET "/apps/:app-id" [app-id]
         (apps/get-app app-id))

    (DELETE "/apps/:app-id" [app-id :as req]
            (delete-app req app-id))

    (PATCH "/apps/:app-id" [app-id :as req]
           (update-app-labels req app-id))

    (PUT "/apps/:app-id" [app-id :as req]
         (update-app req app-id))

    (POST "/apps/:app-id/copy" [app-id :as req]
          (copy-app req app-id))

    (GET "/apps/:app-id/details" [app-id]
         (apps/get-app-details app-id))

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

    (POST "/tools" [:as req]
          (import-tools req))

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

   (PATCH "/analysis/:analysis-id" [analysis-id :as {body :body}]
          (apps/update-job analysis-id body))

   (GET "/default-output-dir" []
        (get-default-output-dir))

   (POST "/default-output-dir" [:as {body :body}]
         (reset-default-output-dir body))

   (PUT "/feedback" [:as {body :body}]
        (provide-user-feedback body))))
