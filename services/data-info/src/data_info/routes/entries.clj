(ns data-info.routes.entries
  (:use [compojure.api.sweet]
        [data-info.routes.domain.common]
        [data-info.routes.domain.entry])
  (:require [clojure-commons.error-codes :as ce]
            [data-info.services.entry :as entry]))


(defroutes* entries

  (context* "/entries" []
    :tags ["Entries"]

    (HEAD* "/id/:entry-id" [:as {uri :uri}]
      :path-params [entry-id :- DataIdPathParam]
      :query [{:keys [user]} SecuredQueryParamsRequired]
      :responses {200 {:description "User has read permissions for given data item."}
                  403 {:description "User does not have read permissions for given data item."}
                  404 {:description "Entry ID does not exist."}
                  422 {:description "User does not exist or an internal error occurred."}}
      :summary "Entry Meta-Status"
      :description "Returns an HTTP status according to the user's access level to the data item."
      (ce/trap uri entry/id-entry entry-id user))

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
      {:status 501})))
