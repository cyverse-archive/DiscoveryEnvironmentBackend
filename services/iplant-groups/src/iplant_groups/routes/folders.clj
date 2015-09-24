(ns iplant_groups.routes.folders
  (:use [common-swagger-api.schema]
        [iplant_groups.routes.domain.folder]
        [iplant_groups.routes.domain.privileges]
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
        (service/trap uri folders/folder-search params))

  (POST* "/" [:as {:keys [uri]}]
        :return      Folder
        :query       [params StandardUserQueryParams]
        :body        [body (describe BaseFolder "The folder to add.")]
        :summary     "Add Folder"
        :description "This endpoint allows adding a new folder."
        (service/trap uri folders/add-folder body params))

  (context* "/:folder-id" []
    :path-params [folder-id :- FolderIdPathParam]

    (GET* "/" [:as {:keys [uri]}]
          :query       [params StandardUserQueryParams]
          :return      Folder
          :summary     "Get Folder Information"
          :description "This endpoint allows callers to get information about a single folder."
          (service/trap uri folders/get-folder folder-id params))

    (DELETE* "/" [:as {:keys [uri]}]
          :query       [params StandardUserQueryParams]
          :return      FolderStub
          :summary     "Delete Folder"
          :description "This endpoint allows deleting a folder if the current user has permissions to do so."
          (service/trap uri folders/delete-folder folder-id params))

    (GET* "/privileges" [:as {:keys [uri]}]
          :query       [params StandardUserQueryParams]
          :return      FolderPrivileges
          :summary     "List Folder Privileges"
          :description "This endpoint allows callers to list the privileges visible to the current user of a single folder."
          (service/trap uri folders/get-folder-privileges folder-id params))))
