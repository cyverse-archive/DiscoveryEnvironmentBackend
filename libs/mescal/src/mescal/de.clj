(ns mescal.de
  (:require [mescal.agave-de-v2 :as v2]
            [mescal.core :as core]))

(defprotocol DeAgaveClient
  "An Agave client with customizations that are specific to the discovery environment."
  (hpcAppGroup [this])
  (listApps [this])
  (getApp [this app-id]))

(deftype DeAgaveClientV2 [agave jobs-enabled? irods-home]
  DeAgaveClient
  (hpcAppGroup [this]
    (v2/hpc-app-group))
  (listApps [this]
    (v2/list-apps agave jobs-enabled?))
  (getApp [this app-id]
    (v2/get-app agave app-id)))

(defn de-agave-client-v2
  [base-url token-info jobs-enabled? irods-home & {:keys [timeout] :or {timeout 5000}}]
  (DeAgaveClientV2.
   (core/agave-client-v2 base-url token-info :timeout timeout)
   jobs-enabled?
   irods-home))
