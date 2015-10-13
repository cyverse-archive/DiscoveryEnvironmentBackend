(ns data-info.routes.stats
  (:use [common-swagger-api.schema]
        [data-info.routes.domain.common]
        [data-info.routes.domain.stats])
  (:require [data-info.services.stat :as stat]
            [data-info.util.service :as svc]
            [schema.core :as s]))


(defroutes* stat-gatherer

            ; FIXME Update metadactyl exception handling when data-info excptn hndlg updated
            ; metadactyl catches exceptions thrown from this EP.
  (context* "/stat-gatherer" []
    :tags ["bulk"]

    (POST* "/" [:as {uri :uri}]
      :query [params StandardUserQueryParams]
      :body [body (describe Paths "The paths to gather status information on.")]
      :return (s/either StatResponse StatusInfo)
      :summary "File and Folder Status Information"
      :description (str
"This endpoint allows the caller to get information about many files and folders at once."
(get-error-code-block
  "ERR_DOES_NOT_EXIST, ERR_NOT_READABLE, ERR_NOT_A_USER, ERR_TOO_MANY_RESULTS"))
      (svc/trap uri stat/do-stat params body))))
