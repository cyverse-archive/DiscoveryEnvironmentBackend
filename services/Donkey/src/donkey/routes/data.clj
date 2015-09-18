(ns donkey.routes.data
  (:use [compojure.core]
        [donkey.services.file-listing]
        [donkey.services.sharing :only [share unshare]]
        [donkey.auth.user-attributes]
        [donkey.util])
  (:require [donkey.util.config :as config]
            [donkey.services.filesystem.garnish.controllers :as garnish]
            [donkey.clients.data-info :as data]
            [donkey.clients.saved-searches :as saved]))

(defn secured-data-routes
  "The routes for data sharing endpoints."
  []
  (optional-routes
   [config/data-routes-enabled]

   (POST "/filetypes/type" [:as {:keys [body params]}]
         (garnish/add-type body params))

   (GET "/filetypes/type-list" []
        (data/get-type-list))

   (POST "/share" [:as req]
         (share req))

   (POST "/unshare" [:as req]
         (unshare req))

   (GET "/saved-searches" []
        (saved/get-saved-searches (:username current-user)))

   (POST "/saved-searches" [:as {:keys [body]}]
         (saved/set-saved-searches (:username current-user) body))

   (DELETE "/saved-searches" []
           (saved/delete-saved-searches (:username current-user)))))
