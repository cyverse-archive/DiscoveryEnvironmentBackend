(ns metadata.routes.favorites
  (:use [compojure.api.sweet]
        [metadata.routes.domain.common]
        [metadata.routes.domain.favorites])
  (:require [metadata.services.favorites :as fave]
            [metadata.util.service :as service]))

(defroutes* favorites
  (context* "/favorites" []
    :tags ["favorites"]

    (GET* "/filesystem" [:as {uri :uri}]
      :query [{:keys [user entity-type]} FavoritesDataListingParams]
      :return (describe DataIdList "The UUIDs of the favorite files and folders of the user.")
      :summary "List Favorite Data Resources"
      :description "This endpoint lists IDs for the authenticated user's favorite files and folders."
      (service/trap uri fave/list-favorite-data-ids user entity-type))

    (DELETE* "/filesystem/:data-id" [:as {uri :uri}]
      :path-params [data-id :- TargetIdPathParam]
      :query [{:keys [user]} StandardQueryParams]
      :summary "Unmark a Data Resource as Favorite"
      :description "This endpoint removes a file or folder from the authenticated user's favorites."
      (service/trap uri fave/remove-favorite user data-id))

   (PUT* "/filesystem/:data-id" [:as {uri :uri}]
     :path-params [data-id :- TargetIdPathParam]
     :query [{:keys [user data-type]} StandardDataItemQueryParams]
     :summary "Mark a Data Resource as Favorite"
     :description "This endpoint marks a given file or folder a favorite of the authenticated user."
     (service/trap uri fave/add-favorite user data-id data-type))

   (POST* "/filter" [:as {uri :uri}]
     :query [{:keys [user]} StandardQueryParams]
     :body [body (describe DataIdList "The UUIDs for the files and folders to be filtered.")]
     :return
     (describe DataIdList
       "The UUIDs from the request that correspond to favorite files and folders of the user.")
     :summary "Filter a Set of Resources for Favorites"
     :description "
This endpoint is used to indicate which resources in a provided set of resources are favorites of
the authenticated user."
     (service/trap uri fave/filter-favorites user body))))
