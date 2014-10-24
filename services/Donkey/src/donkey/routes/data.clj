(ns donkey.routes.data
  (:use [compojure.core]
        [donkey.services.file-listing]
        [donkey.services.sharing :only [share unshare]]
        [donkey.auth.user-attributes]
        [donkey.util])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]
            [donkey.services.filesystem.garnish.controllers :as garnish]
            [donkey.clients.saved-searches :as saved]))

(defn secured-data-routes
  "The routes for data sharing endpoints."
  []
  (optional-routes
   [config/data-routes-enabled]

   (GET "/filetypes/type" [:as {:keys [uri params]}]
        (ce/trap uri #(garnish/get-types params)))

   (POST "/filetypes/type" [:as {:keys [uri body params]}]
         (ce/trap uri #(garnish/add-type body params)))

   (DELETE "/filetypes/type" [:as {:keys [uri params]}]
           (ce/trap uri #(garnish/delete-type params)))

   (GET "/filetypes/type-list" [:as {:keys [uri]}]
        (ce/trap uri #(garnish/get-type-list)))

   (GET "/filetypes/type/paths" [:as {:keys [uri params]}]
        (ce/trap uri #(garnish/find-typed-paths params)))

   (GET "/filetypes/auto-type" [:as {:keys [uri params]}]
        (ce/trap uri #(garnish/preview-auto-type params)))

   (POST "/filetypes/auto-type" [:as {:keys [uri body params]}]
         (ce/trap uri #(garnish/set-auto-type body params)))

   (POST "/share" [:as {:keys [uri] :as req}]
         (ce/trap uri #(share req)))

   (POST "/unshare" [:as {:keys [uri] :as req}]
         (ce/trap #(unshare req)))

   (GET "/saved-searches" [:as {:keys [uri]}]
        (ce/trap uri #(saved/get-saved-searches (:username current-user))))

   (POST "/saved-searches" [:as {:keys [uri body]}]
         (ce/trap uri #(saved/set-saved-searches (:username current-user) body)))

   (DELETE "/saved-searches" [:as {:keys [uri]}]
           (ce/trap uri
                    (fn []
                      (saved/delete-saved-searches (:username current-user))
                      {})))))
