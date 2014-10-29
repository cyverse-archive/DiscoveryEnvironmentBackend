(ns donkey.routes.collaborator
  (:use [compojure.core]
        [donkey.services.collaborators]
        [donkey.util])
  (:require [donkey.util.config :as config]))

(defn secured-collaborator-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/collaborators" [:as req]
        (get-collaborators req))

   (POST "/collaborators" [:as req]
         (add-collaborators req))

   (POST "/remove-collaborators" [:as req]
         (remove-collaborators req))))
