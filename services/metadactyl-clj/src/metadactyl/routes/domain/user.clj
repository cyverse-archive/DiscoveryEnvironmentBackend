(ns metadactyl.routes.domain.user
  (:use [metadactyl.routes.params :only [SecuredQueryParams]]
        [ring.swagger.schema :only [describe]]
        [schema.core :only [defschema]])
  (:import [java.util Date UUID]))

(defschema User
  {:id       (describe UUID "The DE's internal user identifier")
   :username (describe String "The user's iPlant username")})

(defschema Users
  {:users (describe [User] "The list of users")})

(defschema UserIds
  {:ids (describe [UUID] "The list of user IDs")})

(defschema LoginParams
  (assoc SecuredQueryParams
    :ip-address (describe String "The IP address obtained from the original request.")
    :user-agent (describe String "The user agent obtained from the original request.")))

(defschema LoginResponse
  {:login_time (describe Long "Login time as milliseconds since the epoch.")})
