(ns metadactyl.routes.collaborators
  (:use [compojure.api.sweet]
        [metadactyl.routes.domain.collaborator]
        [metadactyl.routes.params]
        [metadactyl.user :only [current-user]]
        [ring.swagger.schema :only [describe]])
  (:require [metadactyl.service.collaborators :as collaborators]
            [metadactyl.util.service :as service]))

(defroutes* collaborators
  (GET* "/" [:as {:keys [uri]}]
        :query [params SecuredQueryParams]
        :summary "List Collaborators"
        :return Collaborators
        :notes "This service allows users to retrieve a list of their collaborators."
        (service/trap uri collaborators/get-collaborators current-user))

  (POST* "/" [:as {:keys [uri]}]
         :query [params SecuredQueryParams]
         :summary "Add Collaborators"
         :body [body (describe Collaborators "The collaborators to add.")]
         :notes "This service allows users to add other users to their list of collaborators."
         (service/trap uri collaborators/add-collaborators current-user body))

  (POST* "/shredder" [:as {:keys [uri]}]
         :query [params SecuredQueryParams]
         :summary "Remove Collaborators"
         :body [body (describe Collaborators "The collaborators to remove.")]
         :notes "This service allows users to remove other users from their list of
         collaborators."
         (service/trap uri collaborators/remove-collaborators current-user body)))
