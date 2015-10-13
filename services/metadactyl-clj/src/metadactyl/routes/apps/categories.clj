(ns metadactyl.routes.apps.categories
  (:use [common-swagger-api.schema]
        [metadactyl.routes.domain.app.category]
        [metadactyl.routes.params]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.coercions :only [coerce!]]
        [ring.util.http-response :only [ok]])
  (:require [metadactyl.service.apps :as apps]
            [metadactyl.util.service :as service]
            [compojure.route :as route]))

(defroutes* app-categories
  (GET* "/" []
        :query [params CategoryListingParams]
        :return AppCategoryListing
        :summary "List App Categories"
        :description "This service is used by the DE to obtain the list of app categories that
         are visible to the user."
        (ok (apps/get-app-categories current-user params)))

  (GET* "/:category-id" []
        :path-params [category-id :- AppCategoryIdPathParam]
        :query [params AppListingPagingParams]
        :return AppCategoryAppListing
        :summary "List Apps in a Category"
        :description "This service lists all of the apps within an app category or any of its
         descendents. The DE uses this service to obtain the list of apps when a user
         clicks on a category in the _Apps_ window.
         This endpoint accepts optional URL query parameters to limit and sort Apps,
         which will allow pagination of results."
        (ok (coerce! AppCategoryAppListing
                 (apps/list-apps-in-category current-user category-id params))))

  (route/not-found (service/unrecognized-path-response)))
