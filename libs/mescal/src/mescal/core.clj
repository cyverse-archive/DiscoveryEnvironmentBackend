(ns mescal.core
  (:require [mescal.agave-v2 :as v2]))

(defprotocol AgaveClient
  "A client for the Agave API."
  (listSystems [_])
  (listApps [_])
  (getApp [_ app-id])
  (submitJob [_ submission])
  (listJobs [_] [_ job-ids])
  (listJob [_ job-id]))

(deftype AgaveClientV2 [base-url token-info-fn timeout]
  AgaveClient
  (listSystems [_]
    (v2/check-access-token token-info-fn timeout)
    (v2/list-systems base-url token-info-fn timeout))
  (listApps [_]
    (v2/check-access-token token-info-fn timeout)
    (v2/list-apps base-url token-info-fn timeout))
  (getApp [_ app-id]
    (v2/check-access-token token-info-fn timeout)
    (v2/get-app base-url token-info-fn timeout app-id))
  (submitJob [_ submission]
    (v2/check-access-token token-info-fn timeout)
    (v2/submit-job base-url token-info-fn timeout submission))
  (listJobs [_]
    (v2/check-access-token token-info-fn timeout)
    (v2/list-jobs base-url token-info-fn timeout))
  (listJobs [_ job-ids]
    (v2/check-access-token token-info-fn timeout)
    (v2/list-jobs base-url token-info-fn timeout job-ids))
  (listJob [_ job-id]
    (v2/check-access-token token-info-fn timeout)
    (v2/list-job base-url token-info-fn timeout job-id)))

(defn agave-client-v2
  [base-url token-info-fn & {:keys [timeout] :or {timeout 5000}}]
  (let [token-info-wrapper-fn (memoize #(ref (token-info-fn)))]
    (AgaveClientV2. base-url token-info-wrapper-fn timeout)))
