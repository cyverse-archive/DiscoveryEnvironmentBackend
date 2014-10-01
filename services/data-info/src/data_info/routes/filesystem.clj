(ns data-info.routes.filesystem
  (:use [compojure.core]
        [data-info.util])
  (:require [data-info.util.config :as config]
            [data-info.services.filesystem.create :as create]
            [data-info.services.filesystem.directory :as dir]
            [data-info.services.filesystem.exists :as exists]
            [data-info.services.filesystem.home :as home]
            [data-info.services.filesystem.metadata :as meta]
            [data-info.services.filesystem.metadata-templates :as mt]
            [data-info.services.filesystem.move :as move]
            [data-info.services.filesystem.page-csv :as csv]
            [data-info.services.filesystem.page-file :as file]
            [data-info.services.filesystem.preview :as preview]
            [data-info.services.filesystem.rename :as rename]
            [data-info.services.filesystem.root :as root]
            [data-info.services.filesystem.sharing :as sharing]
            [data-info.services.filesystem.space-handling :as sh]
            [data-info.services.filesystem.stat :as stat]
            [data-info.services.filesystem.tickets :as ticket]
            [data-info.services.filesystem.trash :as trash]
            [data-info.services.filesystem.updown :as ud]
            [data-info.services.filesystem.users :as user]
            [data-info.services.filesystem.uuids :as uuid]))


(defn filesystem-routes
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
