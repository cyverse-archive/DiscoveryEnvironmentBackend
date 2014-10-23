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

   (GET "/sessions" [:as {:keys [uri]}]
        (ce/trap uri user-session))

   (POST "/sessions" [:as {:keys [uri body]}]
         (ce/trap uri #(user-session (slurp body))))

   (DELETE "/sessions" [:as {:keys [uri]}]
           (ce/trap uri remove-session))))
