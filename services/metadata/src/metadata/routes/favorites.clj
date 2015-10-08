(ns metadata.routes.favorites
  (:use [common-swagger-api.schema]
        [metadata.routes.domain.common]
        [metadata.routes.domain.favorites]
        [ring.util.http-response :only [ok]])
  (:require [metadata.services.favorites :as fave]))

(defroutes* favorites
  (context* "/favorites" []
    :tags ["favorites"]

    (GET* "/filesystem" []
      :query [{:keys [user entity-type]} FavoritesDataListingParams]
      :return (describe DataIdList "The UUIDs of the favorite files and folders of the user.")
      :summary "List Favorite Data Resources"
      :description "This endpoint lists IDs for the authenticated user's favorite files and folders."
      (ok (fave/list-favorite-data-ids user entity-type)))

    (DELETE* "/filesystem/:data-id" []
      :path-params [data-id :- TargetIdPathParam]
      :query [{:keys [user]} StandardUserQueryParams]
      :summary "Unmark a Data Resource as Favorite"
      :description "This endpoint removes a file or folder from the authenticated user's favorites."
      (ok (fave/remove-favorite user data-id)))

   (PUT* "/filesystem/:data-id" []
     :path-params [data-id :- TargetIdPathParam]
     :query [{:keys [user data-type]} StandardDataItemQueryParams]
     :summary "Mark a Data Resource as Favorite"
     :description "This endpoint marks a given file or folder a favorite of the authenticated user."
     (ok (fave/add-favorite user data-id data-type)))

   (POST* "/filter" []
     :query [{:keys [user]} StandardUserQueryParams]
     :body [body (describe DataIdList "The UUIDs for the files and folders to be filtered.")]
     :return
     (describe DataIdList
       "The UUIDs from the request that correspond to favorite files and folders of the user.")
     :summary "Filter a Set of Resources for Favorites"
     :description "
This endpoint is used to indicate which resources in a provided set of resources are favorites of
the authenticated user."
     (ok (fave/filter-favorites user body)))))
