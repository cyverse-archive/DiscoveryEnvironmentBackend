(ns donkey.routes.filesystem
  (:use [compojure.core]
        [donkey.util])
  (:require [donkey.util.config :as config]
            [clojure.tools.logging :as log]
            [donkey.clients.data-info :as data]
            [donkey.services.filesystem.directory :as dir]
            [donkey.services.filesystem.exists :as exists]
            [donkey.services.filesystem.home :as home]
            [donkey.services.filesystem.manifest :as manifest]
            [donkey.services.filesystem.metadata :as meta]
            [donkey.services.filesystem.metadata-template-avus :as mta]
            [donkey.services.filesystem.metadata-templates :as mt]
            [donkey.services.filesystem.root :as root]
            [donkey.services.filesystem.sharing :as sharing]
            [donkey.services.filesystem.stat :as stat]
            [donkey.services.filesystem.tickets :as ticket]
            [donkey.services.filesystem.updown :as ud]))

(defn secured-filesystem-routes
  "The routes for file IO endpoints."
  []
  (optional-routes
    [config/filesystem-routes-enabled]

    (GET "/filesystem/root" [:as req]
      (controller req root/do-root-listing :params))

    (GET "/filesystem/home" [:as req]
      (controller req home/do-homedir :params))

    (POST "/filesystem/exists" [:as req]
      (controller req exists/do-exists :params :body))

    (POST "/filesystem/stat" [:as req]
      (controller req stat/do-stat :params :body))

    (GET "/filesystem/display-download" [:as req]
      (controller req ud/do-special-download :params))

    (GET "/filesystem/directory" [:as req]
      (controller req dir/do-directory :params))

    (GET "/filesystem/paged-directory" [:as req]
      (controller req dir/do-paged-listing :params))

    (POST "/filesystem/directories" [:as req]
      (controller req data/create-dirs :params :body))

    (POST "/filesystem/directory/create" [:as req]
      (controller req data/create-dir :params :body))

    (POST "/filesystem/rename" [:as req]
      (controller req data/rename :params :body))

    (POST "/filesystem/delete" [:as req]
      (controller req data/delete-paths :params :body))

    (POST "/filesystem/delete-contents" [:as req]
      (controller req data/delete-contents :params :body))

    (POST "/filesystem/move" [:as req]
      (controller req data/move :params :body))

    (POST "/filesystem/move-contents" [:as req]
      (controller req data/move-contents :params :body))

    (GET "/filesystem/file/manifest" [:as req]
      (controller req manifest/do-manifest :params))

    (GET "/filesystem/metadata" [:as req]
      (controller req meta/do-metadata-get :params))

    (POST "/filesystem/metadata" [:as req]
      (controller req meta/do-metadata-set :params :body))

    (DELETE "/filesystem/metadata" [:as req]
      (controller req meta/do-metadata-delete :params))

    (POST "/filesystem/metadata-batch" [:as req]
      (controller req meta/do-metadata-batch-set :params :body))

    (POST "/filesystem/metadata-batch-add" [:as req]
      (controller req meta/do-metadata-batch-add :params :body))

    (GET "/filesystem/metadata/templates" [:as req]
      (controller req mt/do-metadata-template-list))

    (GET "/filesystem/metadata/template/:id" [id :as req]
      (controller req mt/do-metadata-template-view id))

    (GET "/filesystem/metadata/template/attr/:id" [id :as req]
      (controller req mt/do-metadata-attribute-view id))

    (POST "/filesystem/share" [:as req]
      (controller req sharing/do-share :params :body))

    (POST "/filesystem/unshare" [:as req]
      (controller req sharing/do-unshare :params :body))

    (POST "/filesystem/user-permissions" [:as req]
      (controller req data/collect-permissions :params :body))

    (POST "/filesystem/restore" [:as req]
      (controller req data/restore-files :params :body))

    (POST "/filesystem/restore-all" [:as req]
      (controller req data/restore-files :params))

    (POST "/filesystem/tickets" [:as req]
      (controller req ticket/do-add-tickets :params :body))

    (POST "/filesystem/delete-tickets" [:as req]
      (controller req ticket/do-remove-tickets :params :body))

    (POST "/filesystem/list-tickets" [:as req]
      (controller req ticket/do-list-tickets :params :body))

    (DELETE "/filesystem/trash" [:as req]
      (controller req data/delete-trash :params))

    (POST "/filesystem/read-chunk" [:as req]
      (controller req data/read-chunk :params :body))

    (POST "/filesystem/read-csv-chunk" [:as req]
      (controller req data/read-tabular-chunk :params :body))

    (POST "/filesystem/anon-files" [:as req]
      (controller req sharing/do-anon-files :params :body))))

(defn secured-filesystem-metadata-routes
  "The routes for file metadata endpoints."
  []
  (optional-routes
   [#(and (config/filesystem-routes-enabled)
          (config/metadata-routes-enabled))]

    (POST "/filesystem/metadata/csv-form-parser" [dest :as req]
      (meta/parse-csv-metadata req))

    (POST "/filesystem/metadata/csv-parser" [:as {:keys [user-info params] :as req}]
      (meta/parse-src-file-csv-metadata user-info params))

   (POST "/filesystem/:data-id/metadata/copy" [data-id force :as req]
     (controller req meta/do-metadata-copy :params data-id force :body))

   (POST "/filesystem/:data-id/metadata/save" [data-id :as req]
     (controller req meta/do-metadata-save data-id :params :body))

   (GET "/filesystem/:data-id/template-avus" [data-id :as req]
     (controller req mta/do-metadata-template-avu-list :params data-id))

   (POST "/filesystem/:data-id/template-avus/copy" [data-id force :as req]
     (controller req mta/do-copy-metadata-template-avus :params data-id force :body))

   (GET "/filesystem/:data-id/template-avus/:template-id" [data-id template-id :as req]
     (controller req mta/do-metadata-template-avu-list :params data-id template-id))

   (POST "/filesystem/:data-id/template-avus/:template-id" [data-id template-id :as req]
     (controller req mta/do-set-metadata-template-avus :params data-id template-id :body))

   (DELETE "/filesystem/:data-id/template-avus/:template-id" [data-id template-id :as req]
     (controller req mta/do-remove-metadata-template-avus :params data-id template-id))

   (DELETE "/filesystem/:data-id/template-avus/:template-id/:avu-id" 
     [data-id template-id avu-id :as req]
     (controller req mta/do-remove-metadata-template-avus :params data-id template-id avu-id))))

(defn admin-filesystem-metadata-routes
  "The admin routes for file metadata endpoints."
  []
  (optional-routes
    [#(and (config/admin-routes-enabled)
           (config/filesystem-routes-enabled)
           (config/metadata-routes-enabled))]

    (GET "/filesystem/metadata/templates" [:as req]
      (controller req mt/do-metadata-template-admin-list))

    (POST "/filesystem/metadata/templates" [:as req]
      (controller req mt/do-metadata-template-add :body))

    (POST "/filesystem/metadata/templates/:template-id" [template-id :as req]
      (controller req mt/do-metadata-template-edit template-id :body))

    (DELETE "/filesystem/metadata/templates/:template-id" [template-id :as req]
      (controller req mt/do-metadata-template-delete template-id))))
