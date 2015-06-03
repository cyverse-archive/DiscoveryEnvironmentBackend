(ns metadactyl.routes.users
  (:use [compojure.api.sweet]
        [metadactyl.routes.domain.user]
        [metadactyl.routes.params]
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
        (service/trap uri users/by-id body)))
