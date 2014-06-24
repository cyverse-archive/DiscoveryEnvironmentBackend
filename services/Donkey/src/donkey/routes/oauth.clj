(ns donkey.routes.oauth
  (:use [compojure.core]
        [donkey.util])
  (:require [donkey.services.oauth :as oauth]
            [donkey.util.config :as config]))

(defn secured-oauth-routes
  "These routes are callback routes for OAuth authorization codes. They need to be secured because
   we need to associate the access token that we obtain using the authorization code with the
   user. Because of this. These can't be direct callback routes. Instead, the callbacks need to go
   through a servlet in the Discovery Environment backend."
  []
  (routes
   (GET "/oauth/access-code/agave" [:as {params :params}]
        (trap #(oauth/get-access-token (config/agave-oauth-settings) params)))))
