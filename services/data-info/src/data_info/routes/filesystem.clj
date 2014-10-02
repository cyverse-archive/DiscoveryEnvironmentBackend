(ns data-info.routes.filesystem
  (:use [compojure.core]
        [data-info.util])
  (:require [data-info.services.filesystem.create :as create]
            [data-info.services.filesystem.directory :as dir]
            [data-info.services.filesystem.exists :as exists]
            [data-info.services.filesystem.home :as home]
            [data-info.services.filesystem.metadata :as meta]
            [data-info.services.filesystem.move :as move]
            [data-info.services.filesystem.page-csv :as csv]
            [data-info.services.filesystem.page-file :as file]
            [data-info.services.filesystem.preview :as preview]
            [data-info.services.filesystem.rename :as rename]
            [data-info.services.filesystem.sharing :as sharing]
            [data-info.services.filesystem.space-handling :as sh]
            [data-info.services.filesystem.stat :as stat]
            [data-info.services.filesystem.trash :as trash]
            [data-info.services.filesystem.updown :as ud]
            [data-info.services.filesystem.users :as user]
            [data-info.services.filesystem.uuids :as uuid]))


(defn filesystem-routes
  "The routes for file IO endpoints."
  []
  (GET "/filesystem/home" [:as req]
    "Howdy!"
    #_(controller req home/do-homedir :params))

  (POST "/data/exists" [:as req]
    (controller req exists/do-exists :params :body))

  (POST "/data/stat" [:as req]
    (controller req stat/do-stat :params :body))

  (POST "/data/download" [:as req]
    (controller req ud/do-download :params :body))

  (POST "/data/download-contents" [:as req]
    (controller req ud/do-download-contents :params :body))

  (GET "/data/display-download" [:as req]
    (controller req ud/do-special-download :params))

  (GET "/data/upload" [:as req]
    (controller req ud/do-upload :params))

  (GET "/data/directory" [:as req]
    (controller req dir/do-directory :params))

  (GET "/data/paged-directory" [:as req]
    (controller req dir/do-paged-listing :params))

  (POST "/data/directory/create" [:as req]
    (controller req create/do-create :params :body))

  (POST "/data/rename" [:as req]
    (controller req rename/do-rename :params :body))

  (POST "/data/delete" [:as req]
    (controller req trash/do-delete :params :body))

  (POST "/data/delete-contents" [:as req]
    (controller req trash/do-delete-contents :params :body))

  (POST "/data/move" [:as req]
    (controller req move/do-move :params :body))

  (POST "/data/move-contents" [:as req]
    (controller req move/do-move-contents :params :body))

  (GET "/data/file/preview" [:as req]
    (controller req preview/do-preview :params))

  (GET "/data/metadata" [:as req]
    (controller req meta/do-metadata-get :params))

  (POST "/data/metadata" [:as req]
    (controller req meta/do-metadata-set :params :body))

  (DELETE "/data/metadata" [:as req]
    (controller req meta/do-metadata-delete :params))

  (POST "/data/metadata-batch" [:as req]
    (controller req meta/do-metadata-batch-set :params :body))

  (POST "/data/share" [:as req]
    (controller req sharing/do-share :params :body))

  (POST "/data/unshare" [:as req]
    (controller req sharing/do-unshare :params :body))

  (POST "/data/user-permissions" [:as req]
    (controller req user/do-user-permissions :params :body))

  (GET "/data/groups" [:as req]
    (controller req user/do-groups :params))

  (GET "/data/quota" [:as req]
    (controller req user/do-quota :params))

  (POST "/data/restore" [:as req]
    (controller req trash/do-restore :params :body))

  (POST "/data/restore-all" [:as req]
    (controller req trash/do-restore-all :params))

  (GET "/data/user-trash-dir" [:as req]
    (controller req trash/do-user-trash :params))

  (POST "/data/paths-contain-space" [:as req]
    (controller req sh/do-paths-contain-space :params :body))

  (POST "/data/replace-spaces" [:as req]
    (controller req sh/do-replace-spaces :params :body))

  (DELETE "/data/trash" [:as req]
    (controller req trash/do-delete-trash :params))

  (POST "/data/read-chunk" [:as req]
    (controller req file/do-read-chunk :params :body))

  (POST "/data/overwrite-chunk" [:as req]
    (controller req file/do-overwrite-chunk :params :body))

  (POST "/data/read-csv-chunk" [:as req]
    (controller req csv/do-read-csv-chunk :params :body))

  (POST "/data/anon-files" [:as req]
    (controller req sharing/do-anon-files :params :body))

  (POST "/data/paths-for-uuids" [:as req]
    (controller req uuid/do-paths-for-uuids :params :body))

  (POST "/data/uuids-for-paths" [:as req]
    (controller req uuid/do-uuids-for-paths :params :body)))
