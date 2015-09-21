(ns data-info.routes.trash
  (:use [common-swagger-api.schema]
        [data-info.routes.domain.common]
        [data-info.routes.domain.trash])
  (:require [data-info.services.trash :as trash]
            [data-info.util.service :as svc]))

(defroutes* trash
    (DELETE* "/trash" [:as {uri :uri}]
      :tags ["data"]
      :query [params StandardUserQueryParams]
      :return Trash
      :summary "Empty Trash"
      :description (str
  "Empty the trash of the user provided.")
      (svc/trap uri trash/do-delete-trash params))

    (POST* "/deleter" [:as {uri :uri}]
      :tags ["bulk"]
      :query [params StandardUserQueryParams]
      :body [body (describe Paths "The paths to move to the trash")]
      :return Paths
      :summary "Delete Data Items"
      :description (str
  "Delete the data items with the listed paths."
  (get-error-code-block
    "ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_TOO_MANY_PATHS, ERR_NOT_A_USER"))
      (svc/trap uri trash/do-delete params body))

    (context* "/data/:data-id" []
      :path-params [data-id :- DataIdPathParam]
      :tags ["data-by-id"]

      (DELETE* "/" [:as {uri :uri}]
        :query [params StandardUserQueryParams]
        :return Paths
        :summary "Delete Data Item"
        :description (str
  "Deletes the data item with the provided UUID."
  (get-error-code-block
    "ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_TOO_MANY_PATHS, ERR_NOT_A_USER"))
        (svc/trap uri trash/do-delete-uuid params data-id))

      (DELETE* "/children" [:as {uri :uri}]
        :query [params StandardUserQueryParams]
        :return Paths
        :summary "Delete Data Item Contents"
        :description (str
  "Deletes the contents of the folder with the provided UUID."
  (get-error-code-block
    "ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_TOO_MANY_PATHS, ERR_NOT_A_USER"))
        (svc/trap uri trash/do-delete-uuid-contents params data-id))))
