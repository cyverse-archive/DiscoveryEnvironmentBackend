(ns donkey.routes.callbacks
  (:use [compojure.core]
        [donkey.util :only [optional-routes flagged-routes]])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.services.callbacks :as svc]
            [donkey.util.config :as config]))

(defn- de-callback-routes
  "Callback routes used by the DE."
  []
  (optional-routes
   [config/app-routes-enabled]

   (POST "/notification" [:as {:keys [uri body]}]
         (ce/trap uri #(svc/receive-notification body)))

   (POST "/de-job" [:as {:keys [uri body]}]
         (ce/trap uri #(svc/receive-de-job-status-update body)))))

(defn- agave-callback-routes-enabled
  "Determines if Agave callback routes should be enabled."
  []
  (and (config/agave-enabled) (config/agave-jobs-enabled)))

(defn- agave-callback-routes
  "Callback routes used by Agave."
  []
  (optional-routes
   [agave-callback-routes-enabled]

   (POST "/agave-job/:uuid" [uuid :as {:keys [uri params]}]
         (ce/trap uri #(svc/receive-agave-job-status-update uuid params)))))

(defn unsecured-callback-routes
  "All unsecured callback routes."
  []
  (context "/callbacks" []
           (flagged-routes
            (de-callback-routes)
            (agave-callback-routes))))
