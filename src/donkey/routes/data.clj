(ns donkey.routes.data
  (:use [compojure.core]
        [donkey.services.file-listing]
        [donkey.services.sharing :only [share unshare]]
        [donkey.auth.user-attributes]
        [donkey.util])
  (:require [donkey.util.config :as config]
            [donkey.services.garnish.controllers :as garnish]))

(defn secured-data-routes
  "The routes for data sharing endpoints."
  []
  (optional-routes
   [config/data-routes-enabled]

   (GET "/filetypes/type" [:as req]
        (trap #(garnish/get-types (:params req))))

   (POST "/filetypes/type" [:as req]
         (trap #(garnish/add-type (:body req) (:params req))))

   (DELETE "/filetypes/type" [:as req]
           (trap #(garnish/delete-type (:params req))))

   (GET "/filetypes/type-list" []
        (trap #(garnish/get-type-list)))

   (GET "/filetypes/type/paths" [:as req]
        (trap #(garnish/find-typed-paths (:params req))))
   
   (GET "/filetypes/auto-type" [:as req]
        (trap #(garnish/preview-auto-type (:params req))))
   
   (POST "/filetypes/auto-type" [:as req]
         (trap #(garnish/set-auto-type (:body req) (:params req))))

   (POST "/share" [:as req]
         (trap #(share req)))

   (POST "/unshare" [:as req]
         (trap #(unshare req)))))
