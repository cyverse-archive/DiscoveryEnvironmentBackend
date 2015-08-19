(ns data-info.routes.data
  (:use [compojure.api.sweet]
        [data-info.routes.domain.common]
        [data-info.routes.domain.data]
        [data-info.routes.domain.stats])
  (:require [data-info.services.create :as create]
            [data-info.services.rename :as rename]
            [data-info.services.metadata :as meta]
            [data-info.util.service :as svc]))

(defroutes* data-operations

  (context* "/data" []
    :tags ["Data Operations"]

    (POST* "/directories" [:as {uri :uri}]
      :query [params SecuredQueryParamsRequired]
      :body [body (describe Paths "The paths to create.")]
      :return Paths
      :summary "Create Directories"
      :description (str
"Creates a directory, as well as any intermediate directories that do not already exist, given as a
path in the request. For example, if the path `/tempZone/home/rods/test1/test2/test3` is given in
the request, the `/tempZone/home/rods/test1` directory does not exist, and the requesting user has
write permissions on the `/tempZone/home/rods` folder, then all 3 `test*` folders will be created
for the requesting user."
(get-error-code-block
  "ERR_BAD_OR_MISSING_FIELD, ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER"))
      (svc/trap uri create/do-create params body))

    (POST* "/rename" [:as {uri :uri}]
      :query [params SecuredQueryParamsRequired]
      :body [body (describe RenameRequest "The renaming to perform.")]
      :return RenameResult
      :summary "Rename Files"
      :description (str
"Renames a file given two paths."
(get-error-code-block
  "ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_INCOMPLETE_RENAME, ERR_NOT_A_USER, ERR_TOO_MANY_PATHS"))
      (svc/trap uri rename/do-rename params body))

    (POST* "/:data-id/metadata/save" [:as {uri :uri}]
      :path-params [data-id :- DataIdPathParam]
      :query [params SecuredQueryParamsRequired]
      :body [body (describe MetadataSaveRequest "The metadata save request.")]
      :return FileStat
      :summary "Exporting Metadata to a File"
      :description (str
"Exports file/folder details in a JSON format (similar to the /stat-gatherer endpoint response),
including all Metadata Template AVUs and IRODS AVUs visible to the requesting user, to the file
specified in the request."
(get-error-code-block
  "ERR_INVALID_JSON, ERR_EXISTS, ERR_DOES_NOT_EXIST, ERR_NOT_READABLE,"
  "ERR_NOT_WRITEABLE, ERR_NOT_A_USER, ERR_BAD_PATH_LENGTH, ERR_BAD_DIRNAME_LENGTH,"
  "ERR_BAD_BASENAME_LENGTH, ERR_TOO_MANY_RESULTS"))
      (svc/trap uri meta/do-metadata-save data-id params body))))
