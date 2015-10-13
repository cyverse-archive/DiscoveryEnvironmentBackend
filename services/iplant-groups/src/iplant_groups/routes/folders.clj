(ns iplant_groups.routes.folders
  (:use [common-swagger-api.schema]
        [iplant_groups.routes.domain.folder]
        [iplant_groups.routes.domain.privileges]
        [iplant_groups.routes.domain.params]
        [ring.util.http-response :only [ok]])
  (:require [iplant_groups.service.folders :as folders]))

(defroutes* folders
  (GET* "/" []
        :query       [params SearchParams]
        :return      FolderList
        :summary     "Folder Search"
        :description "This endpoint allows callers to search for folders by name. Only folders
        that are visible to the given user will be listed."
        (ok (folders/folder-search params)))

  (POST* "/" []
        :return      Folder
        :query       [params StandardUserQueryParams]
        :body        [body (describe BaseFolder "The folder to add.")]
        :summary     "Add Folder"
        :description "This endpoint allows adding a new folder."
        (ok (folders/add-folder body params)))

  (context* "/:folder-id" []
    :path-params [folder-id :- FolderIdPathParam]

    (GET* "/" []
          :query       [params StandardUserQueryParams]
          :return      Folder
          :summary     "Get Folder Information"
          :description "This endpoint allows callers to get information about a single folder."
          (ok (folders/get-folder folder-id params)))

    (PUT* "/" []
        :return      Folder
        :query       [params StandardUserQueryParams]
        :body        [body (describe FolderUpdate "The folder information to update.")]
        :summary     "Update Folder"
        :description "This endpoint allows callers to update folder information."
        (ok (folders/update-folder folder-id body params)))
    
    (DELETE* "/" []
          :query       [params StandardUserQueryParams]
          :return      FolderStub
          :summary     "Delete Folder"
          :description "This endpoint allows deleting a folder if the current user has permissions to do so."
          (ok (folders/delete-folder folder-id params)))

    (context* "/privileges" []

      (GET* "/" []
            :query       [params StandardUserQueryParams]
            :return      FolderPrivileges
            :summary     "List Folder Privileges"
            :description "This endpoint allows callers to list the privileges visible to the current user of a single folder."
            (ok (folders/get-folder-privileges folder-id params)))

      (context* "/:subject-id/:privilege-name" []
        :path-params [subject-id :- NonBlankString
                      privilege-name :- NonBlankString]

        (PUT* "/" []
              :query       [params StandardUserQueryParams]
              :return      Privilege
              :summary     "Add Folder Privilege"
              :description "This endpoint allows callers to add a specific privilege for a specific subject to a specific folder."
              (ok (folders/add-folder-privilege folder-id subject-id privilege-name params)))

        (DELETE* "/" []
              :query       [params StandardUserQueryParams]
              :return      Privilege
              :summary     "Remove Folder Privilege"
              :description "This endpoint allows callers to remove a specific privilege for a specific subject to a specific folder."
              (ok (folders/remove-folder-privilege folder-id subject-id privilege-name params)))))))
