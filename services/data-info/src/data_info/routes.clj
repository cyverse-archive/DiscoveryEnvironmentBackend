(ns data-info.routes
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [data-info.services.create :as create]
            [data-info.services.directory :as dir]
            [data-info.services.exists :as exists]
            [data-info.services.home :as home]
            [data-info.services.metadata :as meta]
            [data-info.services.move :as move]
            [data-info.services.page-csv :as csv]
            [data-info.services.page-file :as file]
            [data-info.services.preview :as preview]
            [data-info.services.rename :as rename]
            [data-info.services.sharing :as sharing]
            [data-info.services.space-handling :as sh]
            [data-info.services.stat :as stat]
            [data-info.services.trash :as trash]
            [data-info.services.type-detect.controllers :as detect]
            [data-info.services.updown :as ud]
            [data-info.services.users :as user]
            [data-info.services.uuids :as uuid]
            [data-info.services.welcome :as welcome]
            [data-info.util :as util]
            [data-info.util.service :as svc]))


(defroutes all-routes
  (GET "/" [:as req]
    (util/controller req welcome/welcome))

  (POST "/cart" [:as req]
    (util/controller req ud/dispatch-download :params :body))

  (HEAD "/entries/:entry" [:as req]
      (util/controller req exists/exists? :params))

  (POST "/existence-marker" [:as req]
    (util/controller req exists/do-exists :params :body))

  (GET "/home" [:as req]
    (util/controller req home/do-homedir :params))

  (POST "/stat-gatherer" [:as req]
    (util/controller req stat/do-stat :params :body))

  (GET "/data/display-download" [:as req]
    (util/controller req ud/do-special-download :params))

  (GET "/data/upload" [:as req]
    (util/controller req ud/do-upload :params))

  (GET "/data/directory" [:as req]
    (util/controller req dir/do-directory :params))

  (GET "/data/paged-directory" [:as req]
    (util/controller req dir/do-paged-listing :params))

  (POST "/data/directory/create" [:as req]
    (util/controller req create/do-create :params :body))

  (POST "/data/rename" [:as req]
    (util/controller req rename/do-rename :params :body))

  (POST "/data/delete" [:as req]
    (util/controller req trash/do-delete :params :body))

  (POST "/data/delete-contents" [:as req]
    (util/controller req trash/do-delete-contents :params :body))

  (POST "/data/move" [:as req]
    (util/controller req move/do-move :params :body))

  (POST "/data/move-contents" [:as req]
    (util/controller req move/do-move-contents :params :body))

  (GET "/data/file/preview" [:as req]
    (util/controller req preview/do-preview :params))

  (GET "/data/metadata" [:as req]
    (util/controller req meta/do-metadata-get :params))

  (POST "/data/metadata" [:as req]
    (util/controller req meta/do-metadata-set :params :body))

  (DELETE "/data/metadata" [:as req]
    (util/controller req meta/do-metadata-delete :params))

  (POST "/data/metadata-batch" [:as req]
    (util/controller req meta/do-metadata-batch-set :params :body))

  (POST "/data/share" [:as req]
    (util/controller req sharing/do-share :params :body))

  (POST "/data/unshare" [:as req]
    (util/controller req sharing/do-unshare :params :body))

  (POST "/data/user-permissions" [:as req]
    (util/controller req user/do-user-permissions :params :body))

  (GET "/data/groups" [:as req]
    (util/controller req user/do-groups :params))

  (GET "/data/quota" [:as req]
    (util/controller req user/do-quota :params))

  (POST "/data/restore" [:as req]
    (util/controller req trash/do-restore :params :body))

  (POST "/data/restore-all" [:as req]
    (util/controller req trash/do-restore-all :params))

  (GET "/data/user-trash-dir" [:as req]
    (util/controller req trash/do-user-trash :params))

  (POST "/data/paths-contain-space" [:as req]
    (util/controller req sh/do-paths-contain-space :params :body))

  (POST "/data/replace-spaces" [:as req]
    (util/controller req sh/do-replace-spaces :params :body))

  (DELETE "/data/trash" [:as req]
    (util/controller req trash/do-delete-trash :params))

  (POST "/data/read-chunk" [:as req]
    (util/controller req file/do-read-chunk :params :body))

  (POST "/data/overwrite-chunk" [:as req]
    (util/controller req file/do-overwrite-chunk :params :body))

  (POST "/data/read-csv-chunk" [:as req]
    (util/controller req csv/do-read-csv-chunk :params :body))

  (POST "/data/anon-files" [:as req]
    (util/controller req sharing/do-anon-files :params :body))

  (POST "/data/paths-for-uuids" [:as req]
    (util/controller req uuid/do-paths-for-uuids :params :body))

  (POST "/data/uuids-for-paths" [:as req]
    (util/controller req uuid/do-uuids-for-paths :params :body))

  (GET "/filetypes/type" [:as req]
    (util/trap #(detect/get-types (:params req))))

  (POST "/filetypes/type" [:as req]
    (util/trap #(detect/add-type (:body req) (:params req))))

  (DELETE "/filetypes/type" [:as req]
    (util/trap #(detect/delete-type (:params req))))

  (GET "/filetypes/type-list" []
    (util/trap #(detect/get-type-list)))

  (GET "/filetypes/type/paths" [:as req]
    (util/trap #(detect/find-typed-paths (:params req))))

  (GET "/filetypes/auto-type" [:as req]
    (util/trap #(detect/preview-auto-type (:params req))))

  (POST "/filetypes/auto-type" [:as req]
    (util/trap #(detect/set-auto-type (:body req) (:params req))))

  (route/not-found (svc/unrecognized-path-response)))
