(ns metadactyl.routes.domain.user
  (:use [ring.swagger.schema :only [describe]]
        [schema.core :only [defschema]])
  (:import [java.util UUID]))

(defschema User
  {:id       (describe UUID "The DE's internal user identifier")
   :username (describe String "The user's iPlant username")})

(defschema Users
  {:users (describe [User] "The list of users")})

(defschema UserIds
  {:ids (describe [UUID] "The list of user IDs")})
