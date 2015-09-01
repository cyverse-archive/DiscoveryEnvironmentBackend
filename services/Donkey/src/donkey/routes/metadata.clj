(ns donkey.routes.metadata
  (:use [compojure.core]
        [donkey.services.file-listing]
        [donkey.services.metadata.metadactyl]
        [donkey.util])
  (:require [clojure.tools.logging :as log]
            [donkey.clients.metadactyl.raw :as metadactyl]
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
         (service/success-response (metadactyl/get-admin-app-categories params)))

    (POST "/apps/categories" [:as {:keys [body]}]
          (service/success-response (metadactyl/add-category body)))

    (POST "/apps/categories/shredder" [:as {:keys [body]}]
          (service/success-response (metadactyl/delete-categories body)))

    (DELETE "/apps/categories/:category-id" [category-id]
            (service/success-response (metadactyl/delete-category category-id)))

    (PATCH "/apps/categories/:category-id" [category-id :as {:keys [body]}]
           (service/success-response (metadactyl/update-category category-id body)))))

(defn admin-apps-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/app-routes-enabled))]

    (POST "/apps" [:as {:keys [body]}]
          (service/success-response (metadactyl/categorize-apps body)))

    (POST "/apps/shredder" [:as {:keys [body]}]
          (service/success-response (metadactyl/permanently-delete-apps body)))

    (DELETE "/apps/:app-id" [app-id]
            (service/success-response (metadactyl/admin-delete-app app-id)))

    (PATCH "/apps/:app-id" [app-id :as {:keys [body]}]
           (service/success-response (metadactyl/admin-update-app app-id body)))

    (POST "/apps/:app-id/documentation" [app-id :as {:keys [body]}]
          (service/success-response (metadactyl/admin-add-app-docs app-id body)))

    (PATCH "/apps/:app-id/documentation" [app-id :as {:keys [body]}]
           (service/success-response (metadactyl/admin-edit-app-docs app-id body)))))

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
         (service/success-response (metadactyl/get-all-workflow-elements params)))

    (GET "/apps/elements/:element-type" [element-type :as {:keys [params]}]
         (service/success-response (metadactyl/get-workflow-elements element-type params)))

    (POST "/apps/pipelines" [:as {:keys [body]}]
          (service/success-response (metadactyl/add-pipeline body)))

    (PUT "/apps/pipelines/:app-id" [app-id :as {:keys [body]}]
         (service/success-response (metadactyl/update-pipeline app-id body)))

    (POST "/apps/pipelines/:app-id/copy" [app-id]
          (service/success-response (metadactyl/copy-pipeline app-id)))

    (GET "/apps/pipelines/:app-id/ui" [app-id]
         (service/success-response (metadactyl/edit-pipeline app-id)))

    (POST "/apps/shredder" [:as {:keys [body]}]
          (service/success-response (metadactyl/delete-apps body)))

    (GET "/apps/:app-id" [app-id]
         (service/success-response (metadactyl/get-app app-id)))

    (DELETE "/apps/:app-id" [app-id]
            (service/success-response (metadactyl/delete-app app-id)))

    (PATCH "/apps/:app-id" [app-id :as {:keys [body]}]
           (service/success-response (metadactyl/relabel-app app-id body)))

    (PUT "/apps/:app-id" [app-id :as {:keys [body]}]
         (service/success-response (metadactyl/update-app app-id body)))

    (POST "/apps/:app-id/copy" [app-id]
          (service/success-response (metadactyl/copy-app app-id)))

    (GET "/apps/:app-id/details" [app-id]
         (service/success-response (metadactyl/get-app-details app-id)))

    (GET "/apps/:app-id/documentation" [app-id]
         (service/success-response (metadactyl/get-app-docs app-id)))

    (POST "/apps/:app-id/documentation" [app-id :as {:keys [body]}]
          (service/success-response (metadactyl/add-app-docs app-id body)))

    (PATCH "/apps/:app-id/documentation" [app-id :as {:keys [body]}]
           (service/success-response (metadactyl/edit-app-docs app-id body)))

    (DELETE "/apps/:app-id/favorite" [app-id]
            (service/success-response (metadactyl/remove-favorite-app app-id)))

    (PUT "/apps/:app-id/favorite" [app-id]
         (service/success-response (metadactyl/add-favorite-app app-id)))

    (GET "/apps/:app-id/is-publishable" [app-id]
         (service/success-response (metadactyl/app-publishable? app-id)))

    (POST "/apps/:app-id/publish" [app-id :as {:keys [body]}]
          (service/success-response (metadactyl/make-app-public app-id body)))

    (DELETE "/apps/:app-id/rating" [app-id]
            (service/success-response (metadactyl/delete-rating app-id)))

    (POST "/apps/:app-id/rating" [app-id :as {body :body}]
          (service/success-response (metadactyl/rate-app app-id body)))

    (GET "/apps/:app-id/tasks" [app-id]
         (service/success-response (metadactyl/list-app-tasks app-id)))

    (GET "/apps/:app-id/tools" [app-id]
         (service/success-response (metadactyl/get-tools-in-app app-id)))

    (GET "/apps/:app-id/ui" [app-id]
         (service/success-response (metadactyl/get-app-ui app-id)))))

(defn analysis-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/analyses" [:as {:keys [params]}]
        (service/success-response (metadactyl/list-jobs params)))

   (POST "/analyses" [:as {:keys [body]}]
         (service/success-response (metadactyl/submit-job body)))

   (PATCH "/analyses/:analysis-id" [analysis-id :as {body :body}]
          (service/success-response (metadactyl/update-job analysis-id body)))

   (DELETE "/analyses/:analysis-id" [analysis-id]
           (service/success-response (metadactyl/delete-job analysis-id)))

   (POST "/analyses/shredder" [:as {:keys [body]}]
         (service/success-response (metadactyl/delete-jobs body)))

   (GET "/analyses/:analysis-id/parameters" [analysis-id]
        (service/success-response (metadactyl/get-job-params analysis-id)))

   (GET "/analyses/:analysis-id/relaunch-info" [analysis-id]
        (service/success-response (metadactyl/get-job-relaunch-info analysis-id)))

   (GET "/analyses/:analysis-id/steps" [analysis-id]
        (service/success-response (metadactyl/list-job-steps analysis-id)))

   (POST "/analyses/:analysis-id/stop" [analysis-id]
         (service/success-response (metadactyl/stop-job analysis-id)))))

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
         (service/success-response (metadactyl/list-reference-genomes params)))

    (GET "/reference-genomes/:reference-genome-id" [reference-genome-id]
         (service/success-response (metadactyl/get-reference-genome reference-genome-id)))))

(defn admin-tool-routes
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
        (config/app-routes-enabled))]

    (POST "/tools" [:as {:keys [body]}]
          (import-tools body))

    (PATCH "/tools/:tool-id" [tool-id :as {:keys [body]}]
           (metadactyl/update-tool tool-id body))

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

    (GET "/tools" [:as {:keys [params]}]
         (service/success-response (metadactyl/search-tools params)))

    (GET "/tools/:tool-id" [tool-id]
         (service/success-response (metadactyl/get-tool tool-id)))

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
