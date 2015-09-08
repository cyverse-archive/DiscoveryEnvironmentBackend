(ns data-info.routes.trash
  (:use [compojure.api.sweet]
        [data-info.routes.domain.common])
  (:require [data-info.services.trash :as trash]
            [data-info.util.service :as svc]))

(defroutes* trash
    (context* "/data/:data-id" []
      :path-params [data-id :- DataIdPathParam]
      :tags ["data-by-id"]

      (DELETE* "/" [:as {uri :uri}]
        :query [params SecuredQueryParamsRequired]
        :return Paths
        :summary "Delete Data Item"
        :description (str
  "Deletes the data item with the provided UUID."
  (get-error-code-block
    "ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_TOO_MANY_PATHS, ERR_NOT_A_USER"))
        (svc/trap uri trash/do-delete-uuid params data-id))

      (DELETE* "/children" [:as {uri :uri}]
        :query [params SecuredQueryParamsRequired]
        :return Paths
        :summary "Delete Data Item Contents"
        :description (str
  "Deletes the contents of the folder with the provided UUID."
  (get-error-code-block
    "ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_TOO_MANY_PATHS, ERR_NOT_A_USER"))
        (svc/trap uri trash/do-delete-uuid-contents params data-id))))
