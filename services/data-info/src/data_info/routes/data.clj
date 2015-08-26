(ns data-info.routes.data
  (:use [compojure.api.sweet]
        [data-info.routes.domain.common]
        [data-info.routes.domain.data]
        [data-info.routes.domain.stats])
  (:require [data-info.services.create :as create]
            [data-info.services.rename :as rename]
            [data-info.services.metadata :as meta]
            [data-info.services.entry :as entry]
            [clojure-commons.error-codes :as ce]
            [data-info.util.service :as svc]))

(defroutes* data-operations

  (context* "/data" []
    :tags ["Data Operations"]

    (HEAD* "/id/:data-id" [:as {uri :uri}]
      :path-params [data-id :- DataIdPathParam]
      :query [{:keys [user]} SecuredQueryParamsRequired]
      :responses {200 {:description "User has read permissions for given data item."}
                  403 {:description "User does not have read permissions for given data item."}
                  404 {:description "Data Item ID does not exist."}
                  422 {:description "User does not exist or an internal error occurred."}}
      :summary "Data Item Meta-Status"
      :description "Returns an HTTP status according to the user's access level to the data item."
      (ce/trap uri entry/id-entry data-id user))

    (GET* "/path/:zone/*" [:as {{zone :zone path :*} :params uri :uri}]
      :query [params FolderListingParams]
      :no-doc true
      (ce/trap uri entry/dispatch-path-to-resource zone path params))

    ;; This is actually handled by the above route, which cannot be documented properly.
    (GET* "/path/:zone/:path" [:as {uri :uri}]
      :path-params [zone :- (describe String "The IRODS zone")
                    path :- (describe String "The IRODS path under the zone")]
      :query [params FolderListingParams]
      :summary "Entry Contents"
      :description (str
"Lists subdirectories and file details of directory paths, or gets file contents of paths to files.

 Of the optional query parameters, only the `attachment` parameter applies to files, and all others
 only apply to listing directory contents.

 The `limit` parameter is required for paths to directories."
(get-error-code-block
  "ERR_DOES_NOT_EXIST, ERR_NOT_READABLE, ERR_NOT_A_USER,"
  "ERR_BAD_PATH_LENGTH, ERR_BAD_DIRNAME_LENGTH, ERR_BAD_BASENAME_LENGTH"
  "ERR_BAD_QUERY_PARAMETER, ERR_MISSING_QUERY_PARAMETER"))
      {:status 501})

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

    (PUT* "/:data-id/name" [:as {uri :uri}]
      :path-params [data-id :- DataIdPathParam]
      :query [params SecuredQueryParamsRequired]
      :body [body (describe Filename "The new name of the file.")]
      :return RenameResult
      :summary "Change a file's name."
      :description (str
"Moves the file with the provided UUID to a new name within the same folder."
(get-error-code-block
  "ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_INCOMPLETE_RENAME, ERR_NOT_A_USER, ERR_TOO_MANY_PATHS"))
      (svc/trap uri rename/do-rename-uuid params body data-id))

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
