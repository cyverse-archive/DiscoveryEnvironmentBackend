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

(deftype AgaveClientV1 [base-url user token]
  AgaveClient
  (listSystems [this]
    (v1/list-systems base-url))
  (listPublicApps [this]
    (v1/list-public-apps base-url))
  (countPublicApps [this]
    (count (v1/list-public-apps base-url)))
  (listMyApps [this]
    (v1/list-my-apps base-url user token))
  (countMyApps [this]
    (count (v1/list-my-apps base-url user token)))
  (getApp [this app-id]
    (v1/get-app base-url app-id))
  (submitJob [this params]
    (v1/submit-job base-url user token params))
  (submitJob [this app params]
    (v1/submit-job base-url user token app params))
  (listJob [this job-id]
    (v1/list-job base-url user token job-id))
  (listJobs [this]
    (v1/list-jobs base-url user token))
  (listJobs [this job-ids]
    (v1/list-jobs base-url user token job-ids)))

(defn agave-client-v1
  [base-url proxy-user proxy-pass user]
  (let [token (v1/authenticate base-url proxy-user proxy-pass user)]
    (AgaveClientV1. base-url user token)))
