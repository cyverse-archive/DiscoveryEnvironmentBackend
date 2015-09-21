(ns metadata.routes.domain.common
  (:use [common-swagger-api.schema :only [describe StandardUserQueryParams]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def TargetIdPathParam (describe UUID "The target item's UUID"))
(def DataTypeEnum (s/enum "file" "folder"))

(s/defschema StandardDataItemQueryParams
  (assoc StandardUserQueryParams
    :data-type (describe DataTypeEnum "The type of the requested data item.")))

(s/defschema DataIdList
  {:filesystem (describe [UUID] "A list of UUIDs, each for a file or folder")})
