(ns fishy.routes.folders
  (:use [compojure.api.sweet]
        [fishy.routes.domain.folder]
        [fishy.routes.domain.params])
  (:require [fishy.service.folders :as folders]
            [fishy.util.service :as service]))

(defroutes* folders
  (GET* "/" [:as {:keys [uri]}]
        :query       [params SearchParams]
        :return      FolderList
        :summary     "Folder Search"
        :description "This endpoint allows callers to search for folders by name. Only folders
        that are visible to the given user will be listed."
        (service/trap uri folders/folder-search params)))
