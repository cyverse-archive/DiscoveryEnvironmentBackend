(ns data-info.routes.data
  (:use [compojure.core]
        [data-info.util])
  (:require [data-info.util.config :as config]
            [data-info.services.type-detect.controllers :as detect]))


(defn data-routes
  "The routes for data sharing endpoints."
  []
  (GET "/filetypes/type" [:as req]
    (trap #(detect/get-types (:params req))))

  (POST "/filetypes/type" [:as req]
    (trap #(detect/add-type (:body req) (:params req))))

  (DELETE "/filetypes/type" [:as req]
    (trap #(detect/delete-type (:params req))))

  (GET "/filetypes/type-list" []
    (trap #(detect/get-type-list)))

  (GET "/filetypes/type/paths" [:as req]
    (trap #(detect/find-typed-paths (:params req))))
   
  (GET "/filetypes/auto-type" [:as req]
    (trap #(detect/preview-auto-type (:params req))))
   
  (POST "/filetypes/auto-type" [:as req]
    (trap #(detect/set-auto-type (:body req) (:params req)))))
