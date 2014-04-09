(ns donkey.routes.filesystem
  (:use [compojure.core]
        [donkey.auth.user-attributes]
        [donkey.services.filesystem.root]
        [donkey.services.filesystem.directory]
        [donkey.services.filesystem.home]
        [donkey.services.filesystem.rename]
        [donkey.services.filesystem.trash]
        [donkey.services.filesystem.move]
        [donkey.services.filesystem.create]
        [donkey.services.filesystem.metadata]
        [donkey.services.filesystem.metadata-templates]
        [donkey.services.filesystem.sharing]
        [donkey.services.filesystem.preview]
        [donkey.services.filesystem.exists]
        [donkey.services.filesystem.stat]
        [donkey.services.filesystem.manifest]
        [donkey.services.filesystem.updown]
        [donkey.services.filesystem.tickets]
        [donkey.services.filesystem.users]
        [donkey.services.filesystem.space-handling]
        [donkey.services.filesystem.copy]
        [donkey.services.filesystem.page-file]
        [donkey.services.filesystem.page-csv]
        [donkey.util.validators :only [parse-body]]
        [donkey.util.transformers :only [add-current-user-to-map]]
        [donkey.util]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [donkey.util.config :as config]
            [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook!]]))

(defn secured-filesystem-routes
  "The routes for file IO endpoints."
  []
  (optional-routes
    [config/filesystem-routes-enabled]
    (GET "/filesystem/root" [:as req]
         (controller req do-root-listing :params))

    (GET "/filesystem/home" [:as req]
         (controller req do-homedir :params))

    (POST "/filesystem/exists" [:as req]
          (controller req do-exists :params :body))

    (POST "/filesystem/stat" [:as req]
          (controller req do-stat :params :body))

    (POST "/filesystem/download" [:as req]
          (controller req do-download :params :body))

    (POST "/filesystem/download-contents" [:as req]
          (controller req do-download-contents :params :body))

    (GET "/filesystem/display-download" [:as req]
         (controller req do-special-download :params))

    (GET "/filesystem/upload" [:as req]
         (controller req do-upload :params))

    (GET "/filesystem/directory" [:as req]
         (controller req do-directory :params))

    (GET "/filesystem/paged-directory" [:as req]
         (controller req do-paged-listing :params))

    (POST "/filesystem/directory/create" [:as req]
          (controller req do-create :params :body))

    (POST "/filesystem/rename" [:as req]
          (controller req do-rename :params :body))

    (POST "/filesystem/delete" [:as req]
          (controller req do-delete :params :body))

    (POST "/filesystem/delete-contents" [:as req]
          (controller req do-delete-contents :params :body))

    (POST "/filesystem/move" [:as req]
          (controller req do-move :params :body))

    (POST "/filesystem/move-contents" [:as req]
          (controller req do-move-contents :params :body))

    (GET "/filesystem/file/preview" [:as req]
         (controller req do-preview :params))

    (GET "/filesystem/file/manifest" [:as req]
         (controller req do-manifest :params))

    (GET "/filesystem/metadata" [:as req]
         (controller req do-metadata-get :params))

    (POST "/filesystem/metadata" [:as req]
          (controller req do-metadata-set :params :body))

    (DELETE "/filesystem/metadata" [:as req]
            (controller req do-metadata-delete :params))

    (POST "/filesystem/metadata-batch" [:as req]
          (controller req do-metadata-batch-set :params :body))

    (GET "/filesystem/metadata/templates" [:as req]
         (controller req do-metadata-template-list))

    (GET "/filesystem/metadata/template/:id" [id :as req]
         (controller req do-metadata-template-view id))

    (GET "/filesystem/metadata/template/attr/:id" [id :as req]
         (controller req do-metadata-attribute-view id))

    (POST "/filesystem/share" [:as req]
          (controller req do-share :params :body))

    (POST "/filesystem/unshare" [:as req]
          (controller req do-unshare :params :body))

    (POST "/filesystem/user-permissions" [:as req]
          (controller req do-user-permissions :params :body))

    (GET "/filesystem/groups" [:as req]
         (controller req do-groups :params))

    (GET "/filesystem/quota" [:as req]
         (controller req do-quota :params))

    (POST "/filesystem/restore" [:as req]
          (controller req do-restore :params :body))

    (POST "/filesystem/restore-all" [:as req]
          (controller req do-restore-all :params))

    (POST "/filesystem/tickets" [:as req]
          (controller req do-add-tickets :params :body))

    (POST "/filesystem/delete-tickets" [:as req]
          (controller req do-remove-tickets :params :body))

    (POST "/filesystem/list-tickets" [:as req]
          (controller req do-list-tickets :params :body))

    (GET "/filesystem/user-trash-dir" [:as req]
         (controller req do-user-trash :params))

    (POST "/filesystem/paths-contain-space" [:as req]
          (controller req do-paths-contain-space :params :body))

    (POST "/filesystem/replace-spaces" [:as req]
          (controller req do-replace-spaces :params :body))

    (DELETE "/filesystem/trash" [:as req]
            (controller req do-delete-trash :params))

    (POST "/filesystem/read-chunk" [:as req]
          (controller req do-read-chunk :params :body))

    (POST "/filesystem/overwrite-chunk" [:as req]
          (controller req do-overwrite-chunk :params :body))

    (POST "/filesystem/read-csv-chunk" [:as req]
          (controller req do-read-csv-chunk :params :body))))
