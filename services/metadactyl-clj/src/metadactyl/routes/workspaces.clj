(ns metadactyl.routes.workspaces
  (:use [compojure.api.sweet]
        [metadactyl.routes.domain.workspace]
        [metadactyl.routes.params]
        [metadactyl.user :only [current-user]])
  (:require [metadactyl.service.workspace :as workspace]
            [metadactyl.util.service :as service]))

(defroutes* workspaces
  (GET* "/" [:as {:keys [uri]}]
        :query [params SecuredQueryParams]
        :return Workspace
        :summary "Obtain user workspace information."
        :notes "This endpoint returns information about the workspace belonging to the
        authenticated user."
        (service/trap uri workspace/get-workspace current-user)))
