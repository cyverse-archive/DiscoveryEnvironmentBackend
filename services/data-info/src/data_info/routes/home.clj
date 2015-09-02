(ns data-info.routes.home
  (:use [compojure.api.sweet]
        [data-info.routes.domain.common]
        [data-info.routes.domain.stats])
  (:require [data-info.services.home :as home]
            [data-info.util.service :as svc]))

(defroutes* home

  (context* "/home" []
    :tags ["home"]

    (GET* "/" [:as {uri :uri}]
      :query [params SecuredQueryParamsRequired]
      :return PathIdInfo
      :summary "Get User's Home Dir"
      :description (str
"This endpoint returns the ID and path of a user's home directory, creating it if it does not
 already exist."
(get-error-code-block
  "ERR_NOT_A_USER"))
      (svc/trap uri home/do-homedir params))))
