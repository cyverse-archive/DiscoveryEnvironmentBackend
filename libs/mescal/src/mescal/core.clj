(ns mescal.core
  (:require [mescal.agave-v1 :as v1]))

(defprotocol AgaveClient
  "A client for the Agave API."
  (listSystems [this])
  (listPublicApps [this])
  (countPublicApps [this])
  (listMyApps [this])
  (countMyApps [this])
  (getApp [this app-id])
  (submitJob [this params] [this app params])
  (listJob [this job-id])
  (listJobs [this] [this job-ids]))

(deftype AgaveClientV1 [base-url user token-fn]
  AgaveClient
  (listSystems [this]
    (token-fn)
    (v1/list-systems base-url))
  (listPublicApps [this]
    (token-fn)
    (v1/list-public-apps base-url))
  (countPublicApps [this]
    (token-fn)
    (count (v1/list-public-apps base-url)))
  (listMyApps [this]
    (v1/list-my-apps base-url user (token-fn)))
  (countMyApps [this]
    (count (v1/list-my-apps base-url user (token-fn))))
  (getApp [this app-id]
    (token-fn)
    (v1/get-app base-url app-id))
  (submitJob [this params]
    (v1/submit-job base-url user (token-fn) params))
  (submitJob [this app params]
    (v1/submit-job base-url user (token-fn) app params))
  (listJob [this job-id]
    (v1/list-job base-url user (token-fn) job-id))
  (listJobs [this]
    (v1/list-jobs base-url user (token-fn)))
  (listJobs [this job-ids]
    (v1/list-jobs base-url user (token-fn) job-ids)))

(defn agave-client-v1
  [base-url proxy-user proxy-pass user & {:keys [timeout] :or {timeout 5000}}]
  (let [token-fn (memoize #(v1/authenticate base-url proxy-user proxy-pass user timeout))]
    (AgaveClientV1. base-url user token-fn)))
