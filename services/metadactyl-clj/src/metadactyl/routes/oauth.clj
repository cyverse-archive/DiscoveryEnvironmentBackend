(ns metadactyl.routes.oauth
  (:use [metadactyl.routes.domain.oauth]
        [metadactyl.routes.params]
        [compojure.api.sweet])
  (require [clojure-commons.error-codes :as ce]
           [metadactyl.service.oauth :as oauth]
           [metadactyl.util.service :as service]))

(defroutes* oauth
  (GET* "/access-code/:api-name" [:as {uri :uri}]
        :path-params [api-name :- ApiName]
        :query       [params OAuthCallbackQueryParams]
        :return      OAuthCallbackResponse
        :summary     "Obtain an OAuth access token for an authorization code."
        (ce/trap uri #(oauth/get-access-token api-name params))))
