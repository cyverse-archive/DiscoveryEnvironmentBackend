(ns metadactyl.routes.apps
  (:require [metadactyl.app-listings :refer [get-app-groups]]
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
                 are visible to the user. This list includes any app categories that are in a
                 workspace that is marked as public in the database. If the 'public' parameter is
                 not set to 'true', then app categories that are in the user's workspace are
                 included as well."
                 (service/trap #(get-app-groups params)))

           (route/not-found (service/unrecognized-path-response))))

