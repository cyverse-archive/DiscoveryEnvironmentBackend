(ns data-info.routes.domain.trash
  (:use [common-swagger-api.schema :only [describe]]
        [data-info.routes.domain.common])
  (:require [schema.core :as s]))

(s/defschema Trash
  (assoc Paths
         :trash (describe String "The path of the trash directory that was emptied.")))
