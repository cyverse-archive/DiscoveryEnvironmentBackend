(ns data-info.routes.navigation
  (:use [compojure.api.sweet]
        [data-info.routes.domain.common]
        [data-info.routes.domain.navigation]
        [data-info.routes.domain.stats])
  (:require [data-info.services.directory :as dir]
            [data-info.util.service :as svc]))

(defroutes* navigation

  (context* "/navigation" []
    :tags ["Navigation"]

    (GET* "/path/:zone/*" [:as {{path :*} :params uri :uri}]
      :path-params [zone :- String]
      :query [params SecuredQueryParamsRequired]
      :return NavigationResponse
      :summary "Directory List (Non-Recursive): incomplete docs"
      :description
"See alternate endpoint documentation.

This endpoint definition can not be properly documented or used from the current version of the
Swagger UI, but the alternate endpoint can be, and its requests will be processed by this endpoint."
      (svc/trap uri dir/do-directory zone path params))

    (GET* "/path/:zone/:path" [:as {uri :uri}]
      :path-params [zone :- (describe String "The IRODS zone")
                    path :- (describe String "The IRODS path under the zone")]
      :query [params SecuredQueryParamsRequired]
      :return NavigationResponse
      :summary "Directory List (Non-Recursive): documented"
      :description (str
                     "Only lists subdirectories of the directory path passed into it."
                     (get-error-code-block
                       "ERR_DOES_NOT_EXIST, ERR_NOT_READABLE, ERR_NOT_A_USER, ERR_NOT_A_FOLDER"))
      {:status 501})))
