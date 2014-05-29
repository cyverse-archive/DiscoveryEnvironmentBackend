(ns mescal.core
  (:require [mescal.agave-v2 :as v2]))

(defprotocol AgaveClient
  "A client for the Agave API."
  (listSystems [this])
  (listApps [this]))

(deftype AgaveClientV2 [base-url token-info-fn timeout]
  AgaveClient
  (listSystems [this]
    (v2/check-access-token token-info-fn timeout)
    (v2/list-systems base-url token-info-fn timeout))
  (listApps [this]
    (v2/check-access-token token-info-fn timeout)
    (v2/list-apps base-url token-info-fn timeout)))

(defn agave-client-v2
  [base-url token-info-fn & {:keys [timeout] :or {timeout 5000}}]
  (let [token-info-wrapper-fn (memoize #(ref (token-info-fn)))]
    (AgaveClientV2. base-url token-info-wrapper-fn timeout)))
