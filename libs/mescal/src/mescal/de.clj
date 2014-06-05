(ns mescal.de
  (:require [mescal.agave-de-v2 :as v2]
            [mescal.core :as core]))

(defprotocol DeAgaveClient
  "An Agave client with customizations that are specific to the discovery environment."
  (hpcAppGroup [_])
  (listApps [_])
  (getApp [_ app-id])
  (submitJob [_ submission]))

(deftype DeAgaveClientV2 [agave jobs-enabled? irods-home]
  DeAgaveClient
  (hpcAppGroup [_]
    (v2/hpc-app-group))
  (listApps [_]
    (v2/list-apps agave jobs-enabled?))
  (getApp [_ app-id]
    (v2/get-app agave app-id))
  (submitJob [_ submission]
    (v2/submit-job agave irods-home submission)))

(defn de-agave-client-v2
  [base-url token-info jobs-enabled? irods-home & {:keys [timeout] :or {timeout 5000}}]
  (DeAgaveClientV2.
   (core/agave-client-v2 base-url token-info :timeout timeout)
   jobs-enabled?
   irods-home))
