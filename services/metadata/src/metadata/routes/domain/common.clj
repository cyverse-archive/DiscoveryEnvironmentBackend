(ns metadata.routes.domain.common
  (:use [clojure.string :only [blank?]]
        [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(defn ->optional-param
  "Removes a required param from the given schema and re-adds it as an optional param."
  [schema param]
  (-> schema
    (assoc (s/optional-key param) (schema param))
    (dissoc param)))

(def TargetIdPathParam (describe UUID "The target item's UUID"))
(def DataTypeEnum (s/enum "file" "folder"))
(def NonBlankString
  (describe (s/both String (s/pred (complement blank?) 'non-blank-string?)) "A non-blank string."))

(s/defschema StandardQueryParams
  {:user (describe NonBlankString "The username of the authenticated user")})

(s/defschema StandardDataItemQueryParams
  (assoc StandardQueryParams
    :data-type (describe DataTypeEnum "The type of the requested data item.")))

(s/defschema DataIdList
  {:filesystem (describe [UUID] "A list of UUIDs, each for a file or folder")})
