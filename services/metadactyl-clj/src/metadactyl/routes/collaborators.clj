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
        (service/trap uri collaborators/get-collaborators current-user)))
