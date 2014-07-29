(ns metadactyl.routes.apps
  (:require [metadactyl.app-listings :refer [get-only-app-groups]]
            [metadactyl.routes.params :refer :all]
            [metadactyl.util.service :as service]
            [compojure.api.sweet :refer :all]
            [compojure.route :as route]
            [ring.swagger.schema :as ss]
            [schema.core :as s]))

(defroutes* apps
  (context "/categories" []
           (GET* "/" []
                 :query [params SecuredQueryParams]
                 :summary "List App Categories"
                 :notes "This service is used by the DE to obtain the list of app categories that
                 are visible to the user. This list includes app categories that are in the user's
                 workspace along with any app categories that are in a workspace that is marked as
                 public in the database."
                 (service/trap #(get-only-app-groups params)))

           (route/not-found (service/unrecognized-path-response))))
