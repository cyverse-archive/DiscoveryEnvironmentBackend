(ns data-info.routes.filetypes
  (:use [common-swagger-api.schema]
        [data-info.routes.domain.common]
        [data-info.routes.domain.filetypes])
  (:require [data-info.services.filetypes :as filetypes]
            [data-info.util.service :as svc]))

(defroutes* filetypes-operations
  (GET* "/file-types" [:as {uri :uri}]
    :tags ["filetypes"]
    :return TypesList
    :summary "List File Types"
    :description "Lists available file types supported by the underlying library heuristomancer."
    (svc/trap uri filetypes/do-type-list))

  (PUT* "/data/:data-id/type" [:as {uri :uri}]
    :tags ["data-by-id"]
    :query [params StandardUserQueryParams]
    :path-params [data-id :- DataIdPathParam]
    :body [body (describe FileType "The type to set.")]
    :return FileTypeReturn
    :summary "Set File Type"
    :description (str "Set a file's type to a specific value, or unset it with an empty string."
(get-error-code-block "ERR_NOT_OWNER, ERR_BAD_OR_MISSING_FIELD, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_NOT_A_FILE"))
    (svc/trap uri filetypes/do-add-type-uuid params body data-id)))
