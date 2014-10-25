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

    (GET "/apps/categories" [:as {:keys [uri params]}]
         (ce/trap uri #(apps/get-app-categories params)))

    (GET "/apps/categories/:app-group-id" [app-group-id :as {:keys [uri params]}]
         (ce/trap uri #(apps/apps-in-category app-group-id params)))))

(defn apps-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (POST "/admin/apps" [:as {:keys [uri] :as req}]
          (ce/trap uri #(categorize-apps req)))

    (POST "/admin/apps/shredder" [:as {:keys [uri] :as req}]
          (ce/trap uri #(permanently-delete-apps req)))

    (GET "/apps" [:as {:keys [params uri]}]
         (ce/trap uri #(apps/search-apps params)))

    (POST "/apps" [:as {:keys [uri] :as req}]
          (ce/trap uri #(create-app req)))

    (POST "/apps/arg-preview" [:as {:keys [uri] :as req}]
          (ce/trap uri #(preview-args req)))

    (GET "/apps/ids" [:as {:keys [uri]}]
         (ce/trap uri #(get-all-app-ids)))

    (GET "/apps/elements/:element-type" [element-type :as {:keys [uri]}]
         (ce/trap uri #(get-workflow-elements element-type)))

    (POST "/apps/pipelines" [:as {:keys [uri] :as req}]
          (ce/trap uri #(create-pipeline req)))

    (PUT "/apps/pipelines/:app-id" [app-id :as {:keys [uri] :as req}]
         (ce/trap uri #(update-pipeline req app-id)))

    (POST "/apps/pipelines/:app-id/copy" [app-id :as {:keys [uri]}]
          (ce/trap uri #(apps/copy-workflow app-id)))

    (GET "/apps/pipelines/:app-id/ui" [app-id :as {:keys [uri]}]
         (ce/trap uri #(apps/edit-workflow app-id)))

    (POST "/apps/shredder" [:as {:keys [uri] :as req}]
          (ce/trap uri #(delete-apps req)))

    (GET "/apps/:app-id" [app-id :as {:keys [uri]}]
         (ce/trap uri #(apps/get-app app-id)))

    (DELETE "/apps/:app-id" [app-id :as {:keys [uri] :as req}]
            (ce/trap uri #(delete-app req app-id)))

    (PATCH "/apps/:app-id" [app-id :as {:keys [uri] :as req}]
           (ce/trap uri #(update-app-labels req app-id)))

    (PUT "/apps/:app-id" [app-id :as {:keys [uri] :as req}]
         (ce/trap uri #(update-app req app-id)))

    (POST "/apps/:app-id/copy" [app-id :as {:keys [uri] :as req}]
          (ce/trap uri #(copy-app req app-id)))

    (GET "/apps/:app-id/details" [app-id :as {:keys [uri]}]
         (ce/trap uri #(apps/get-app-details app-id)))

    (GET "/apps/:app-id/is-publishable" [app-id :as {:keys [uri]}]
         (ce/trap uri #(app-publishable? app-id)))

    (DELETE "/apps/:app-id/rating" [app-id :as {:keys [uri]}]
            (ce/trap uri #(apps/delete-rating app-id)))

    (POST "/apps/:app-id/rating" [app-id :as {:keys [body uri]}]
          (ce/trap uri #(apps/rate-app body app-id)))

    (GET "/apps/:app-id/tasks" [app-id :as {:keys [uri]}]
         (ce/trap uri #(apps/list-app-tasks app-id)))

    (GET "/apps/:app-id/ui" [app-id :as {:keys [uri]}]
         (ce/trap uri #(edit-app app-id)))))

(defn analysis-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (POST "/analyses" [:as {:keys [body uri]}]
         (ce/trap uri #(apps/submit-job body)))))

(defn tool-routes
  []
  (optional-routes
    [config/app-routes-enabled]

    (POST "/admin/tools" [:as {:keys [uri] :as req}]
          (ce/trap uri #(import-tools req)))

    (GET "/admin/tool-requests" [:as {:keys [params uri]}]
         (ce/trap uri #(admin-list-tool-requests params)))

    (GET "/admin/tool-requests/:request-id" [request-id :as {:keys [uri]}]
         (ce/trap uri #(get-tool-request request-id)))

    (POST "/admin/tool-requests/:request-id/status" [request-id :as {:keys [uri] :as req}]
          (ce/trap uri #(update-tool-request req request-id)))

    (GET "/tool-requests" [:as {:keys [uri]}]
         (ce/trap uri #(list-tool-requests)))

    (POST "/tool-requests" [:as {:keys [uri] :as req}]
          (ce/trap uri #(submit-tool-request req)))

    (GET "/tool-requests/status-codes" [:as {:keys [params uri]}]
         (ce/trap uri #(list-tool-request-status-codes params)))))

(defn secured-metadata-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/bootstrap" [:as {:keys [uri] :as req}]
        (ce/trap uri #(bootstrap req)))

   (GET "/logout" [:as {:keys [params uri]}]
        (ce/trap uri #(logout params)))

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

   (POST "/make-analysis-public" [:as {:keys [uri] :as req}]
         (ce/trap uri #(make-app-public req)))

   (GET "/default-output-dir" [:as {:keys [uri]}]
        (ce/trap uri #(get-default-output-dir)))

   (POST "/default-output-dir" [:as {:keys [uri body]}]
         (ce/trap :uri #(reset-default-output-dir body)))

   (GET "/reference-genomes" [:as {:keys [uri] :as req}]
        (ce/trap uri #(list-reference-genomes req)))

   (PUT "/reference-genomes" [:as {:keys [uri] :as req}]
        (ce/trap uri #(replace-reference-genomes req)))

   (PUT "/feedback" [:as {:keys [body uri]}]
        (ce/trap uri #(provide-user-feedback body)))))

(defn unsecured-metadata-routes
  []
  (optional-routes
   [config/app-routes-enabled]

   (GET "/search-deployed-components/:search-term" [search-term :as {:keys [uri] :as req}]
        (ce/trap uri #(search-deployed-components req search-term)))

   (POST "/delete-categories" [:as {:keys [uri] :as req}]
         (ce/trap uri #(delete-categories req)))

   (GET "/validate-analysis-for-pipelines/:app-id" [app-id :as {:keys [uri] :as req}]
        (ce/trap uri #(validate-app-for-pipelines req app-id)))

   (GET "/get-analysis-categories/:category-set" [category-set :as {:keys [uri] :as req}]
        (ce/trap uri #(get-app-categories req category-set)))

   (POST "/add-analysis-to-group" [:as {:keys [uri] :as req}]
         (ce/trap uri #(add-app-to-group req)))

   (GET "/get-analysis/:app-id" [app-id :as {:keys [uri] :as req}]
        (ce/trap uri #(get-app req app-id)))

   (GET "/export-workflow/:app-id" [app-id :as {:keys [uri] :as req}]
        (ce/trap uri #(export-workflow req app-id)))

   (POST "/export-deployed-components" [:as {:keys [uri] :as req}]
         (ce/trap uri #(export-deployed-components req)))

   (POST "/preview-template" [:as {:keys [uri] :as req}]
         (ce/trap uri #(preview-template req)))

   (POST "/preview-workflow" [:as {:keys [uri] :as req}]
         (ce/trap uri #(preview-workflow req)))))
