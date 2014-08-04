(ns metadactyl.routes.apps
  (:require [kameleon.uuids :as uuid]
            [metadactyl.app-listings :refer [get-app-groups list-apps-in-group]]
            [metadactyl.routes.params :refer :all]
            [metadactyl.util.service :as service]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [ring.swagger.schema :as ss]
            [schema.core :as s]))

(defroutes* apps
  (context "/categories" []
           (GET* "/" []
                 :query [params CategoryListingParams]
                 :summary "List App Categories"
                 :notes "This service is used by the DE to obtain the list of app categories that
                 are visible to the user. If the 'public' parameter is set to 'true', then only app
                 categories that are in a workspace that is marked as public in the database are
                 returned. If the 'public' parameter is set to 'false', then only app categories
                 that are in the user's workspace are returned. If the 'public' parameter is not
                 set, then both public and the user's private categories are returned."
                 (service/trap #(get-app-groups params)))

           (GET* "/:category-id" [category-id]
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
                 (service/trap #(list-apps-in-group (uuid/uuidify category-id) params)))

           (route/not-found (service/unrecognized-path-response))))

