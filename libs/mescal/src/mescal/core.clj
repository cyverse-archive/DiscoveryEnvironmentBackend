(ns mescal.core
  (:require [mescal.agave-v2 :as v2]))

(defprotocol AgaveClient
  "A client for the Agave API."
  (listSystems [_])
  (getSystemInfo [_ system-name])
  (listApps [_])
  (getApp [_ app-id])
  (submitJob [_ submission])
  (listJobs [_] [_ job-ids])
  (listJob [_ job-id])
  (stopJob [_ job-id])
  (fileDownloadUrl [_ file-path])
  (fileListingUrl [_ file-path])
  (agaveUrl [_ file-path])
  (irodsFilePath [_ file-url])
  (agaveFilePath [_ file-url])
  (storageSystem [_]))

(deftype AgaveClientV2 [base-url storage-system token-info-fn timeout]
  AgaveClient
  (listSystems [_]
    (v2/check-access-token token-info-fn timeout)
    (v2/list-systems base-url token-info-fn timeout))
  (getSystemInfo [_ system-name]
    (v2/check-access-token token-info-fn timeout)
    (v2/get-system-info base-url token-info-fn timeout system-name))
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
    (v2/list-job base-url token-info-fn timeout job-id))
  (stopJob [_ job-id]
    (v2/check-access-token token-info-fn timeout)
    (v2/stop-job base-url token-info-fn timeout job-id))
  (fileDownloadUrl [_ file-path]
    (v2/check-access-token token-info-fn timeout)
    (v2/file-path-to-url "media" base-url token-info-fn timeout storage-system file-path))
  (fileListingUrl [_ file-path]
    (v2/check-access-token token-info-fn timeout)
    (v2/file-path-to-url "listings" base-url token-info-fn timeout storage-system file-path))
  (agaveUrl [_ file-path]
    (v2/check-access-token token-info-fn timeout)
    (v2/file-path-to-agave-url base-url token-info-fn timeout storage-system file-path))
  (irodsFilePath [_ file-url]
    (v2/check-access-token token-info-fn timeout)
    (v2/agave-to-irods-path base-url token-info-fn timeout storage-system file-url))
  (agaveFilePath [_ irods-path]
    (v2/check-access-token token-info-fn timeout)
    (v2/irods-to-agave-path base-url token-info-fn timeout storage-system irods-path))
  (storageSystem [_]
    storage-system))

(defn agave-client-v2
  [base-url storage-system token-info-fn & {:keys [timeout] :or {timeout 5000}}]
  (let [token-info-wrapper-fn (memoize #(ref (token-info-fn)))]
    (AgaveClientV2. base-url storage-system token-info-wrapper-fn timeout)))
