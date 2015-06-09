(ns metadactyl.routes.users
  (:use [compojure.api.sweet]
        [metadactyl.routes.domain.user]
        [metadactyl.routes.params]
        [metadactyl.user :only [current-user]]
        [ring.swagger.schema :only [describe]])
  (:require [metadactyl.service.users :as users]
            [metadactyl.util.service :as service]))

(defroutes* users
  (POST* "/by-id" [:as {:keys [uri]}]
         :query [params SecuredQueryParams]
         :body [body (describe UserIds "The list of user IDs to look up.")]
         :return Users
         :summary "Look up usernames"
         :notes "This endpoint returns usernames for internal user IDs."
         (service/trap uri users/by-id body))

  (GET* "/authenticated" [:as {:keys [uri]}]
        :query [params SecuredQueryParams]
        :return User
        :summary "Get the Authenticated User"
        :notes "This endpoint returns information about the authenticated user."
        (service/trap uri users/authenticated current-user))

  (POST* "/login" [:as {:keys [uri]}]
         :query [params LoginParams]
         :return LoginResponse
         :summary "Record a User Login"
         :notes "Donkey calls this service to record when a user logs in."
         (service/trap uri users/login current-user params))

  (POST* "/logout" [:as {:keys [uri]}]
         :query [params LogoutParams]
         :summary "Record a User Logout"
         :notes "Donkey calls this service to record when a user logs out."
         (service/trap uri users/logout current-user params)))
