(ns metadactyl.routes.apps
  (:require [metadactyl.app-listings :refer [get-app-groups
                                             list-apps-in-group
                                             search-apps]]
            [metadactyl.zoidberg :refer [edit-app
                                         copy-app]]
            [metadactyl.routes.params :refer :all]
            [metadactyl.util.service :as service]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [ring.swagger.schema :as ss]
            [schema.core :as s]))

(defroutes* apps
  (GET* "/" []
        :query [params AppSearchParams]
        :summary "Search Apps"
        :notes "This service allows users to search for Apps based on a part of the App name or
        description. The response body contains a \"templates\" array that is in the same format as
        the \"templates\" array in the /apps/categories/:category-id endpoint response."
        (search-apps params))

  (GET* "/:app-id/ui" []
        :path-params [app-id :- AppIdPathParam]
        :query [params SecuredQueryParams]
        :summary "Make an App Available for Editing"
        :notes "The DE uses this service to obtain the App description JSON so that it can be
        edited. The App must have been integrated by the requesting user, and it must not already be
        public."
        (edit-app app-id))

  (POST* "/:app-id/copy" []
         :path-params [app-id :- AppIdPathParam]
         :query [params SecuredQueryParams]
         :summary "Make a Copy of an App Available for Editing"
         :notes "This service can be used to make a copy of an App in the user's workspace."
         (copy-app app-id))

  (context "/categories" []
           (GET* "/" []
                 :query [params CategoryListingParams]
                 :summary "List App Categories"
                 :notes "This service is used by the DE to obtain the list of app categories that
                 are visible to the user."
                 (service/trap #(get-app-groups params)))

           (GET* "/:category-id" []
                 :path-params [category-id :- AppCategoryIdPathParam]
                 :query [params AppListingParams]
                 :summary "List Apps in a Category"
                 :notes "This service lists all of the apps within an app category or any of its
                 descendents. The DE uses this service to obtain the list of apps when a user
                 clicks on a category in the _Apps_ window.
                 This endpoint accepts optional URL query parameters to limit and sort Apps,
                 which will allow pagination of results.
                 The `can_run` flag is calculated by comparing the number of steps in the app to
                 the number of steps that have deployed component associated with them. If the
                 numbers are different then this flag is set to `false`. The idea is that every
                 step in the analysis has to have, at the very least, a deployed component
                 associated with it in order to run successfully."
                 (service/trap #(list-apps-in-group category-id params))))

  (route/not-found (service/unrecognized-path-response)))

