(ns metadactyl.routes.collaborators
  (:use [common-swagger-api.schema]
        [metadactyl.routes.domain.collaborator]
        [metadactyl.routes.params]
        [metadactyl.user :only [current-user]])
  (:require [metadactyl.service.collaborators :as collaborators]
            [metadactyl.util.service :as service]))

(defroutes* collaborators
  (GET* "/" [:as {:keys [uri]}]
        :query [params SecuredQueryParams]
        :summary "List Collaborators"
        :return Collaborators
        :description "This service allows users to retrieve a list of their collaborators."
        (service/trap uri collaborators/get-collaborators current-user))

  (POST* "/" [:as {:keys [uri]}]
         :query [params SecuredQueryParams]
         :summary "Add Collaborators"
         :body [body (describe Collaborators "The collaborators to add.")]
         :description "This service allows users to add other users to their list of collaborators."
         (service/trap uri collaborators/add-collaborators current-user body))

  (POST* "/shredder" [:as {:keys [uri]}]
         :query [params SecuredQueryParams]
         :summary "Remove Collaborators"
         :body [body (describe Collaborators "The collaborators to remove.")]
         :description "This service allows users to remove other users from their list of
         collaborators."
         (service/trap uri collaborators/remove-collaborators current-user body)))
