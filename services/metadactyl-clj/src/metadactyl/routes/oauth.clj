(ns metadactyl.routes.oauth
  (:use [common-swagger-api.schema]
        [metadactyl.routes.domain.oauth]
        [metadactyl.routes.params]
        [ring.util.http-response :only [ok]])
  (require [metadactyl.service.oauth :as oauth]))

(defroutes* oauth
  (GET* "/access-code/:api-name" []
        :path-params [api-name :- ApiName]
        :query       [params OAuthCallbackQueryParams]
        :return      OAuthCallbackResponse
        :summary     "Obtain an OAuth access token for an authorization code."
        (ok (oauth/get-access-token api-name params))))
