(ns donkey.routes.callbacks
  (:use [compojure.core]
        [donkey.util :only [optional-routes flagged-routes]])
  (:require [donkey.services.callbacks :as svc]
            [donkey.util.config :as config]))

(defn- de-callback-routes
  "Callback routes used by the DE."
  []
  (optional-routes
   [config/app-routes-enabled]

   (POST "/notification" [:as {body :body}]
         (svc/receive-notification body))))

(defn unsecured-callback-routes
  "All unsecured callback routes."
  []
  (context "/callbacks" []
           (flagged-routes
            (de-callback-routes))))
