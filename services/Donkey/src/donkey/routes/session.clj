(ns donkey.routes.session
  (:use [compojure.core]
        [donkey.services.user-sessions]
        [donkey.util])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]))

(defn secured-session-routes
  []
  (optional-routes
   [config/session-routes-enabled]

   (GET "/sessions" []
        (user-session))

   (POST "/sessions" [:as {body :body}]
         (user-session (slurp body)))

   (DELETE "/sessions" []
           (remove-session))))
