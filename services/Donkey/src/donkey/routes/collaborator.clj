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
        (trap #(get-collaborators req)))

   (POST "/collaborators" [:as req]
         (trap #(add-collaborators req)))

   (POST "/remove-collaborators" [:as req]
         (trap #(remove-collaborators req)))))
