(ns donkey.routes.metadata
  (:use [compojure.core]
        [donkey.services.file-listing]
        [donkey.services.metadata.metadactyl]
        [donkey.util.service]
        [donkey.util])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]
            [donkey.services.metadata.apps :as apps]
            [donkey.services.jex :as jex]))

(defn secured-metadata-routes
  []
  (optional-routes
   [config/metadata-routes-enabled]

   (GET "/bootstrap" [:as req]
        (trap #(bootstrap req)))

   (GET "/logout" [:as req]
        (trap #(logout req)))

   (GET "/template/:app-id" [app-id :as req]
        (trap #(get-app-secured req app-id)))

   (GET "/app/:app-id" [app-id]
        (trap #(apps/get-app app-id)))

   (GET "/app-details/:app-id" [app-id]
        (trap #(apps/get-app-details app-id)))

   (PUT "/workspaces/:workspace-id/newexperiment" [workspace-id :as {body :body}]
        (trap #(apps/submit-job workspace-id body)))

   (GET "/workspaces/:workspace-id/executions/list" [_ :as {params :params}]
        (trap #(apps/list-jobs params)))

   (PUT "/workspaces/:workspace-id/executions/delete" [_ :as {body :body}]
        (trap #(apps/delete-jobs body)))

   (PATCH "/analysis/:analysis-id" [analysis-id :as {body :body}]
          (trap #(apps/update-job analysis-id body)))

   (GET "/get-property-values/:job-id" [job-id]
        (trap #(apps/get-property-values job-id)))

   (GET "/app-rerun-info/:job-id" [job-id]
        (trap #(apps/get-app-rerun-info job-id)))

   (DELETE "/stop-analysis/:uuid" [uuid :as req]
           (trap #(jex/stop-analysis req uuid)))

   (POST "/rate-analysis" [:as {body :body}]
         (trap #(apps/rate-app body)))

   (POST "/delete-rating" [:as {body :body}]
         (trap #(apps/delete-rating body)))

   (GET "/search-analyses" [:as {params :params}]
        (trap #(apps/search-apps params)))

   (GET "/app-groups" []
        (trap #(apps/get-only-app-groups)))

   (GET "/get-analyses-in-group/:app-group-id" [app-group-id :as {params :params}]
        (ce/trap "get-analyses-in-group" #(apps/apps-in-group app-group-id params)))

   (GET "/list-analyses-for-pipeline/:app-group-id" [app-group-id]
        (trap #(apps/apps-in-group app-group-id)))

   (GET "/get-components-in-analysis/:app-id" [app-id]
        (trap #(apps/get-deployed-components-in-app app-id)))

   (POST "/update-favorites" [:as {body :body}]
         (trap #(apps/update-favorites body)))

   (GET "/edit-template/:app-id" [app-id :as req]
        (trap #(edit-app req app-id)))

   (GET "/edit-app/:app-id" [app-id :as req]
        (trap #(edit-app-new-format req app-id)))

   (GET "/edit-workflow/:app-id" [app-id :as req]
        (trap #(edit-workflow req app-id)))

   (GET "/copy-template/:app-id" [app-id :as req]
        (trap #(copy-app req app-id)))

   (GET "/copy-workflow/:app-id" [app-id :as req]
        (trap #(copy-workflow req app-id)))

   (PUT "/update-template" [:as req]
        (trap #(update-template-secured req)))

   (PUT "/update-app" [:as req]
        (trap #(update-app-secured req)))

   (POST "/update-workflow" [:as req]
         (trap #(update-workflow-secured req)))

   (POST "/make-analysis-public" [:as req]
         (trap #(make-app-public req)))

   (GET "/is-publishable/:app-id" [app-id]
        (trap #(app-publishable? app-id)))

   (POST "/permanently-delete-workflow" [:as req]
         (trap #(permanently-delete-workflow req)))

   (POST "/delete-workflow" [:as req]
         (trap #(delete-workflow req)))

   (GET "/default-output-dir" []
        (trap #(get-default-output-dir)))

   (POST "/default-output-dir" [:as {body :body}]
         (trap #(reset-default-output-dir body)))

   (GET "/reference-genomes" [:as req]
        (trap #(list-reference-genomes req)))

   (PUT "/reference-genomes" [:as req]
        (trap #(replace-reference-genomes req)))

   (PUT "/tool-request" [:as req]
        (trap #(submit-tool-request req)))

   (POST "/tool-request" [:as req]
         (trap #(update-tool-request-secured req)))

   (GET "/tool-requests" [:as req]
        (trap #(list-tool-requests req)))

   (PUT "/feedback" [:as {body :body}]
        (trap #(provide-user-feedback body)))))

(defn unsecured-metadata-routes
  []
  (optional-routes
   [config/metadata-routes-enabled]

   (GET "/get-workflow-elements/:element-type" [element-type :as req]
        (trap #(get-workflow-elements req element-type)))

   (GET "/search-deployed-components/:search-term" [search-term :as req]
        (trap #(search-deployed-components req search-term)))

   (GET "/get-all-analysis-ids" [:as req]
        (trap #(get-all-app-ids req)))

   (POST "/delete-categories" [:as req]
         (trap #(delete-categories req)))

   (GET "/validate-analysis-for-pipelines/:app-id" [app-id :as req]
        (trap #(validate-app-for-pipelines req app-id)))

   (GET "/analysis-data-objects/:app-id" [app-id :as req]
        (trap #(get-data-objects-for-app req app-id)))

   (POST "/categorize-analyses" [:as req]
         (trap #(categorize-apps req)))

   (GET "/get-analysis-categories/:category-set" [category-set :as req]
        (trap #(get-app-categories req category-set)))

   (POST "/can-export-analysis" [:as req]
         (trap #(can-export-app req)))

   (POST "/add-analysis-to-group" [:as req]
         (trap #(add-app-to-group req)))

   (GET "/get-analysis/:app-id" [app-id :as req]
        (trap #(get-app req app-id)))

   (GET "/public-app-groups" [req]
        (trap #(get-public-app-groups req)))

   (GET "/list-analysis/:app-id" [app-id :as req]
        (trap #(list-app req app-id)))

   (GET "/export-template/:template-id" [template-id :as req]
        (trap #(export-template req template-id)))

   (GET "/export-workflow/:app-id" [app-id :as req]
        (trap #(export-workflow req app-id)))

   (POST "/export-deployed-components" [:as req]
         (trap #(export-deployed-components req)))

   (POST "/preview-template" [:as req]
         (trap #(preview-template req)))

   (POST "/preview-workflow" [:as req]
         (trap #(preview-workflow req)))

   (POST "/update-template" [:as req]
         (trap #(update-template req)))

   (POST "/force-update-workflow" [:as req]
         (trap #(force-update-workflow req)))

   (POST "/update-workflow" [:as req]
         (trap #(update-workflow req)))

   (POST "/update-app-labels" [:as req]
         (trap #(update-app-labels req)))

   (POST "/import-template" [:as req]
         (trap #(import-template req)))

   (POST "/import-workflow" [:as req]
         (trap #(import-workflow req)))

   (POST "/import-tools" [:as req]
         (trap #(import-tools req)))

   (POST "/update-analysis" [:as req]
         (trap #(update-app req)))

   (POST "/submit-tool-request" [:as req]
         (trap #(submit-tool-request req)))

   (POST "/tool-request" [:as req]
         (trap #(update-tool-request req)))

   (GET "/tool-request/:uuid" [uuid :as req]
        (trap #(get-tool-request req uuid)))

   (GET "/tool-requests" [:as {params :params}]
        (trap #(admin-list-tool-requests params)))

   (GET "/tool-request-status-codes" [:as {params :params}]
        (trap #(list-tool-request-status-codes params)))

   (POST "/arg-preview" [:as req]
         (trap #(preview-args req)))))
