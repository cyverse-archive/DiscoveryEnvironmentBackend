(ns data-info.routes.entries
  (:use [compojure.api.sweet]
        [data-info.routes.domain.common]
        [data-info.routes.domain.entry])
  (:require [clojure-commons.error-codes :as ce]
            [data-info.services.entry :as entry]))


(defroutes* entries

  (context* "/entries" []
    :tags ["Entries"]

    (GET* "/path/:zone/*" [:as {{zone :zone path :*} :params uri :uri}]
      :query [params FolderListingParams]
      :summary "Entry Contents: incomplete docs"
      :description
"See alternate endpoint documentation.

This endpoint definition can not be properly documented or used from the current version of the
Swagger UI, but the alternate endpoint can be, and its requests will be processed by this endpoint."
      (ce/trap uri #(entry/dispatch-path-to-resource zone path params)))

    (GET* "/path/:zone/:path" [:as {uri :uri}]
      :path-params [zone :- (describe String "The IRODS zone")
                    path :- (describe String "The IRODS path under the zone")]
      :query [params FolderListingParams]
      :summary "Entry Contents: documented"
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
