(ns data-info.routes.filetypes
  (:use [compojure.api.sweet]
        [data-info.routes.domain.filetypes])
  (:require [data-info.services.filetypes :as filetypes]
            [data-info.util.service :as svc]))

(defroutes* filetypes-operations
  (GET* "/file-types" [:as {uri :uri}]
    :tags ["filetypes"]
    :return TypesList
    :summary "List File Types"
    :description "Lists available file types supported by the underlying library heuristomancer."
    (svc/trap uri filetypes/do-type-list)))
