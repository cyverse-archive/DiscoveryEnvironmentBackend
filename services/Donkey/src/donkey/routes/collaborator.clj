(ns donkey.routes.collaborator
  (:use [compojure.core]
        [donkey.services.collaborators]
        [donkey.util :only [optional-routes]])
  (:require [donkey.clients.metadactyl.raw :as metadactyl]
            [donkey.util.config :as config]
            [donkey.util.service :as service]))

(defn secured-collaborator-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/collaborators" []
        (service/success-response (metadactyl/get-collaborators)))

   (POST "/collaborators" [:as {:keys [body]}]
         (service/success-response (metadactyl/add-collaborators body)))

   (POST "/remove-collaborators" [:as req]
         (remove-collaborators req))))
