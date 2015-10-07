(ns metadactyl.routes.domain.user
  (:use [common-swagger-api.schema :only [describe]]
        [metadactyl.routes.params :only [SecuredQueryParams]]
        [schema.core :only [defschema]])
  (:import [java.util UUID]))

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

(defschema LogoutParams
  (assoc SecuredQueryParams
    :ip-address (describe String "The IP address obtained from the original request.")
    :login-time (describe Long "The login time returned by POST /users/login.")))
