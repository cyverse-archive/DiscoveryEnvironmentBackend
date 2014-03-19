(ns donkey.routes.session
  (:use [compojure.core]
        [donkey.services.user-sessions]
        [donkey.util])
  (:require [donkey.util.config :as config]))

(defn secured-session-routes
  []
  (optional-routes
   [config/session-routes-enabled]

   (GET "/sessions" []
        (trap user-session))

   (POST "/sessions" [:as {body :body}]
         (trap #(user-session (slurp body))))

   (DELETE "/sessions" []
           (trap remove-session))))
