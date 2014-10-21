(ns donkey.routes.metadata
  (:use [compojure.core]
        [donkey.services.file-listing]
        [donkey.services.metadata.metadactyl]
        [donkey.util.service]
        [donkey.util])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]
            [donkey.services.metadata.apps :as apps]))

(defn app-category-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (GET "/apps/categories" [:as {params :params}]
         (trap #(apps/get-app-categories params)))

    (GET "/apps/categories/:app-group-id" [app-group-id :as {params :params}]
         (ce/trap "get-analyses-in-group" #(apps/apps-in-category app-group-id params)))))

(defn apps-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (POST "/admin/apps" [:as req]
          (trap #(categorize-apps req)))

    (POST "/admin/apps/shredder" [:as req]
          (trap #(permanently-delete-apps req)))

    (GET "/apps" [:as {params :params}]
         (trap #(apps/search-apps params)))

    (POST "/apps" [:as {:keys [uri] :as req}]
          (ce/trap uri #(create-app req)))

    (POST "/apps/arg-preview" [:as req]
          (trap #(preview-args req)))

    (GET "/apps/ids" []
         (trap #(get-all-app-ids)))

    (GET "/apps/elements/:element-type" [element-type]
         (trap #(get-workflow-elements element-type)))

    (POST "/apps/pipelines" [:as req]
          (trap #(create-pipeline req)))

    (PUT "/apps/pipelines/:app-id" [app-id :as req]
         (trap #(update-pipeline req app-id)))

    (POST "/apps/pipelines/:app-id/copy" [app-id]
          (trap #(apps/copy-workflow app-id)))

    (GET "/apps/pipelines/:app-id/ui" [app-id]
         (trap #(apps/edit-workflow app-id)))

    (POST "/apps/shredder" [:as req]
          (trap #(delete-apps req)))

    (GET "/apps/:app-id" [app-id :as {:keys [uri]}]
         (ce/trap uri #(apps/get-app app-id)))

    (DELETE "/apps/:app-id" [app-id :as req]
            (trap #(delete-app req app-id)))

    (PATCH "/apps/:app-id" [app-id :as req]
           (trap #(update-app-labels req app-id)))

    (PUT "/apps/:app-id" [app-id :as req]
         (trap #(update-app req app-id)))

    (POST "/apps/:app-id/copy" [app-id :as {:keys [uri] :as req}]
          (ce/trap uri #(copy-app req app-id)))

    (GET "/apps/:app-id/details" [app-id]
         (trap #(apps/get-app-details app-id)))

    (GET "/apps/:app-id/file-parameters" [app-id :as {:keys [uri]}]
         (ce/trap uri #(apps/list-app-file-parameters app-id)))

    (GET "/apps/:app-id/is-publishable" [app-id]
         (trap #(app-publishable? app-id)))

    (DELETE "/apps/:app-id/rating" [app-id]
            (trap #(apps/delete-rating app-id)))

    (POST "/apps/:app-id/rating" [app-id :as {body :body}]
          (trap #(apps/rate-app body app-id)))

    (GET "/apps/:app-id/ui" [app-id]
         (trap #(edit-app app-id)))))

(defn tool-request-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (GET "/admin/tool-requests" [:as {params :params}]
         (trap #(admin-list-tool-requests params)))

    (GET "/admin/tool-requests/:request-id" [request-id]
         (trap #(get-tool-request request-id)))

    (POST "/admin/tool-requests/:request-id/status" [request-id :as req]
         (trap #(update-tool-request req request-id)))

    (GET "/tool-requests" []
         (trap #(list-tool-requests)))

    (POST "/tool-requests" [:as req]
          (trap #(submit-tool-request req)))

    (GET "/tool-requests/status-codes" [:as {params :params}]
         (trap #(list-tool-request-status-codes params)))))

(defn secured-metadata-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/bootstrap" [:as req]
        (trap #(bootstrap req)))

   (GET "/logout" [:as {params :params}]
        (trap #(logout params)))

   (PUT "/workspaces/:workspace-id/newexperiment" [workspace-id :as {:keys [body uri]}]
        (ce/trap uri #(apps/submit-job workspace-id body)))

   (GET "/workspaces/:workspace-id/executions/list" [_ :as {:keys [params uri]}]
        (ce/trap uri #(apps/list-jobs params)))

   (PUT "/workspaces/:workspace-id/executions/delete" [_ :as {:keys [body uri]}]
        (ce/trap uri #(apps/delete-jobs body)))

   (PATCH "/analysis/:analysis-id" [analysis-id :as {:keys [body uri]}]
          (ce/trap uri #(apps/update-job analysis-id body)))

   (GET "/get-property-values/:job-id" [job-id :as {:keys [uri]}]
        (ce/trap uri #(apps/get-property-values job-id)))

   (GET "/app-rerun-info/:job-id" [job-id :as {:keys [uri]}]
        (ce/trap uri #(apps/get-app-rerun-info job-id)))

   (DELETE "/stop-analysis/:uuid" [uuid :as {:keys [uri]}]
           (ce/trap uri #(apps/stop-job uuid)))

   (GET "/get-components-in-analysis/:app-id" [app-id :as {:keys [uri]}]
        (ce/trap uri #(apps/get-deployed-components-in-app app-id)))

   (POST "/update-favorites" [:as {:keys [uri body]}]
         (ce/trap uri #(apps/update-favorites body)))

   (POST "/make-analysis-public" [:as req]
         (trap #(make-app-public req)))

   (GET "/default-output-dir" []
        (trap #(get-default-output-dir)))

   (POST "/default-output-dir" [:as {body :body}]
         (trap #(reset-default-output-dir body)))

   (GET "/reference-genomes" [:as req]
        (trap #(list-reference-genomes req)))

   (PUT "/reference-genomes" [:as req]
        (trap #(replace-reference-genomes req)))

   (PUT "/feedback" [:as {body :body}]
        (trap #(provide-user-feedback body)))))

(defn unsecured-metadata-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/search-deployed-components/:search-term" [search-term :as req]
        (trap #(search-deployed-components req search-term)))

   (POST "/delete-categories" [:as req]
         (trap #(delete-categories req)))

   (GET "/validate-analysis-for-pipelines/:app-id" [app-id :as req]
        (trap #(validate-app-for-pipelines req app-id)))

   (GET "/get-analysis-categories/:category-set" [category-set :as req]
        (trap #(get-app-categories req category-set)))

   (POST "/add-analysis-to-group" [:as req]
         (trap #(add-app-to-group req)))

   (GET "/get-analysis/:app-id" [app-id :as req]
        (trap #(get-app req app-id)))

   (GET "/export-workflow/:app-id" [app-id :as req]
        (trap #(export-workflow req app-id)))

   (POST "/export-deployed-components" [:as req]
         (trap #(export-deployed-components req)))

   (POST "/preview-template" [:as req]
         (trap #(preview-template req)))

   (POST "/preview-workflow" [:as req]
         (trap #(preview-workflow req)))

   (POST "/import-tools" [:as req]
         (trap #(import-tools req)))))
