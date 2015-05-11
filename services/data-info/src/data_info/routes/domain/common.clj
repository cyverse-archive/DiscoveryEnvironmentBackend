(ns data-info.routes.domain.common
  (:use [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def DataIdPathParam (describe UUID "The data items's UUID"))

(s/defschema SecuredQueryParamsRequired
  {:user (describe String "The IRODS username of the requesting user")})

(s/defschema Paths
  {:paths (describe [String] "A list of IRODS paths")})
