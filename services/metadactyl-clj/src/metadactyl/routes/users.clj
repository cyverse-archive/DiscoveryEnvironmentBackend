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
        (service/trap uri users/authenticated current-user)))
