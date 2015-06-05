(ns metadata.routes.domain.common
  (:use [clojure.string :only [blank?]]
        [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def TargetIdPathParam (describe UUID "The target item's UUID"))

(def NonBlankString
  (describe (s/both String (s/pred (complement blank?) 'non-blank-string?)) "A non-blank string."))

(s/defschema StandardQueryParams
  {:user (describe NonBlankString "The username of the authenticated user")})

(s/defschema UserIdParams
  (assoc StandardQueryParams
    :user-id (describe UUID "The user ID from the app database")))

(s/defschema StandardDataItemQueryParams
  (assoc StandardQueryParams
    :data-type (describe (s/enum "file" "folder") "The type of the requested data item.")))

(s/defschema DataIdList
  {:filesystem (describe [UUID] "A list of UUIDs, each for a file or folder")})
