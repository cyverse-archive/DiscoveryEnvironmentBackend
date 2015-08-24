(ns iplant_groups.routes.folders
  (:use [compojure.api.sweet]
        [iplant_groups.routes.domain.folder]
        [iplant_groups.routes.domain.params])
  (:require [iplant_groups.service.folders :as folders]
            [iplant_groups.util.service :as service]))

(defroutes* folders
  (GET* "/" [:as {:keys [uri]}]
        :query       [params SearchParams]
        :return      FolderList
        :summary     "Folder Search"
        :description "This endpoint allows callers to search for folders by name. Only folders
        that are visible to the given user will be listed."
        (service/trap uri folders/folder-search params)))
