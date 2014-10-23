(ns donkey.routes.collaborator
  (:use [compojure.core]
        [donkey.services.collaborators]
        [donkey.util])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]))

(defn secured-collaborator-routes
  []
  (optional-routes
   [config/collaborator-routes-enabled]

   (GET "/collaborators" [:as {:keys [uri] :as req}]
        (ce/trap uri #(get-collaborators req)))

   (POST "/collaborators" [:as {:keys [uri] :as req}]
         (ce/trap uri #(add-collaborators req)))

   (POST "/remove-collaborators" [:as {:keys [uri] :as req}]
         (ce/trap uri #(remove-collaborators req)))))
