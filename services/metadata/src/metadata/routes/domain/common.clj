(ns metadata.routes.domain.common
  (:use [clojure.string :only [blank?]]
        [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def NonBlankString
  (describe (s/both String (s/pred (complement blank?) 'non-blank-string?)) "A non-blank string."))

(s/defschema StandardQueryParams
  {:user (describe String "The username of the authenticated user")})

(s/defschema UserIdParams
  (assoc StandardQueryParams
    :user-id (describe UUID "The user ID from the app database")))
