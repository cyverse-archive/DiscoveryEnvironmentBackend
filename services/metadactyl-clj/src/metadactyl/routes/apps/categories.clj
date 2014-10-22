(ns metadactyl.routes.apps.categories
  (:use [metadactyl.app-listings :only [get-app-groups list-apps-in-group]]
        [metadactyl.routes.domain.app.category]
        [metadactyl.routes.params]
        [compojure.api.sweet])
  (:require [metadactyl.util.service :as service]
            [compojure.route :as route]))

(defroutes* app-categories
  (GET* "/" []
        :query [params CategoryListingParams]
        :return AppCategoryListing
        :summary "List App Categories"
        :notes "This service is used by the DE to obtain the list of app categories that
         are visible to the user."
        (service/trap #(get-app-groups params)))

  (GET* "/:category-id" []
        :path-params [category-id :- AppCategoryIdPathParam]
        :query [params SecuredPagingParamsEmailRequired]
        :return AppCategoryAppListing
        :summary "List Apps in a Category"
        :notes "This service lists all of the apps within an app category or any of its
         descendents. The DE uses this service to obtain the list of apps when a user
         clicks on a category in the _Apps_ window.
         This endpoint accepts optional URL query parameters to limit and sort Apps,
         which will allow pagination of results."
        (service/trap #(list-apps-in-group category-id params)))

  (route/not-found (service/unrecognized-path-response)))
