(ns data-info.routes.domain.common
  (:use [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def NonBlankString
  (describe
    (s/both String (s/pred (complement clojure.string/blank?) 'non-blank?))
    "A non-blank string."))

(def DataIdPathParam (describe UUID "The data items's UUID"))

(s/defschema SecuredQueryParamsRequired
  {:user (describe NonBlankString "The IRODS username of the requesting user")})

(s/defschema Paths
  {:paths (describe [NonBlankString] "A list of IRODS paths")})
