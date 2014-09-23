(ns donkey.routes.filesystem
  (:use [compojure.core]
        [donkey.auth.user-attributes]
        [donkey.util.validators :only [parse-body]]
        [donkey.util.transformers :only [add-current-user-to-map]]
        [donkey.util]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [donkey.util.config :as config]
            [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook!]]
            [donkey.services.filesystem.create :as create]
            [donkey.services.filesystem.directory :as dir]
            [donkey.services.filesystem.exists :as exists]
            [donkey.services.filesystem.home :as home]
            [donkey.services.filesystem.manifest :as manifest]
            [donkey.services.filesystem.metadata :as meta]
            [donkey.services.filesystem.metadata-template-avus :as mta]
            [donkey.services.filesystem.metadata-templates :as mt]
            [donkey.services.filesystem.move :as move]
            [donkey.services.filesystem.page-csv :as csv]
            [donkey.services.filesystem.page-file :as file]
            [donkey.services.filesystem.preview :as preview]
            [donkey.services.filesystem.rename :as rename]
            [donkey.services.filesystem.root :as root]
            [donkey.services.filesystem.sharing :as sharing]
            [donkey.services.filesystem.space-handling :as sh]
            [donkey.services.filesystem.stat :as stat]
            [donkey.services.filesystem.tickets :as ticket]
            [donkey.services.filesystem.trash :as trash]
            [donkey.services.filesystem.updown :as ud]
            [donkey.services.filesystem.users :as user]
            [donkey.services.filesystem.uuids :as uuid]))


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

    (POST "/filesystem/download" [:as req]
      (controller req ud/do-download :params :body))

    (POST "/filesystem/download-contents" [:as req]
      (controller req ud/do-download-contents :params :body))

    (GET "/filesystem/display-download" [:as req]
      (controller req ud/do-special-download :params))

    (GET "/filesystem/upload" [:as req]
      (controller req ud/do-upload :params))

    (GET "/filesystem/directory" [:as req]
      (controller req dir/do-directory :params))

    (GET "/filesystem/paged-directory" [:as req]
      (controller req dir/do-paged-listing :params))

    (POST "/filesystem/directory/create" [:as req]
      (controller req create/do-create :params :body))

    (POST "/filesystem/rename" [:as req]
      (controller req rename/do-rename :params :body))

    (POST "/filesystem/delete" [:as req]
      (controller req trash/do-delete :params :body))

    (POST "/filesystem/delete-contents" [:as req]
      (controller req trash/do-delete-contents :params :body))

    (POST "/filesystem/move" [:as req]
      (controller req move/do-move :params :body))

    (POST "/filesystem/move-contents" [:as req]
      (controller req move/do-move-contents :params :body))

    (GET "/filesystem/file/preview" [:as req]
      (controller req preview/do-preview :params))

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
      (controller req user/do-user-permissions :params :body))

    (GET "/filesystem/groups" [:as req]
      (controller req user/do-groups :params))

    (GET "/filesystem/quota" [:as req]
      (controller req user/do-quota :params))

    (POST "/filesystem/restore" [:as req]
      (controller req trash/do-restore :params :body))

    (POST "/filesystem/restore-all" [:as req]
      (controller req trash/do-restore-all :params))

    (POST "/filesystem/tickets" [:as req]
      (controller req ticket/do-add-tickets :params :body))

    (POST "/filesystem/delete-tickets" [:as req]
      (controller req ticket/do-remove-tickets :params :body))

    (POST "/filesystem/list-tickets" [:as req]
      (controller req ticket/do-list-tickets :params :body))

    (GET "/filesystem/user-trash-dir" [:as req]
      (controller req trash/do-user-trash :params))

    (POST "/filesystem/paths-contain-space" [:as req]
      (controller req sh/do-paths-contain-space :params :body))

    (POST "/filesystem/replace-spaces" [:as req]
      (controller req sh/do-replace-spaces :params :body))

    (DELETE "/filesystem/trash" [:as req]
      (controller req trash/do-delete-trash :params))

    (POST "/filesystem/read-chunk" [:as req]
      (controller req file/do-read-chunk :params :body))

    (POST "/filesystem/overwrite-chunk" [:as req]
      (controller req file/do-overwrite-chunk :params :body))

    (POST "/filesystem/read-csv-chunk" [:as req]
      (controller req csv/do-read-csv-chunk :params :body))

    (POST "/filesystem/anon-files" [:as req]
      (controller req sharing/do-anon-files :params :body))

    (POST "/filesystem/paths-for-uuids" [:as req]
      (controller req uuid/do-paths-for-uuids :params :body))

    (POST "/filesystem/uuids-for-paths" [:as req]
      (controller req uuid/do-uuids-for-paths :params :body))))

(defn secured-filesystem-metadata-routes
  "The routes for file metadata endpoints."
  []
  (optional-routes
   [#(and (config/filesystem-routes-enabled)
          (config/metadata-routes-enabled))]

   (GET "/filesystem/:data-id/template-avus" [data-id :as req]
     (controller req mta/do-metadata-template-avu-list :params data-id))

   (GET "/filesystem/:data-id/template-avus/:template-id" [data-id template-id :as req]
     (controller req mta/do-metadata-template-avu-list :params data-id template-id))

   (POST "/filesystem/:data-id/template-avus/:template-id" [data-id template-id :as req]
     (controller req mta/do-set-metadata-template-avus :params data-id template-id :body))

   (DELETE "/filesystem/:data-id/template-avus/:template-id" [data-id template-id :as req]
     (controller req mta/do-remove-metadata-template-avus :params data-id template-id))

   (DELETE "/filesystem/:data-id/template-avus/:template-id/:avu-id"
     [data-id template-id avu-id :as req]
     (controller req mta/do-remove-metadata-template-avus :params data-id template-id avu-id))))
