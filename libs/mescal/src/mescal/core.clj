(ns mescal.core
  (:require [mescal.agave-v2 :as v2]))

(defprotocol AgaveClient
  "A client for the Agave API."
  (listSystems [this])
  (listApps [this]))

(deftype AgaveClientV2 [base-url token-info-ref timeout]
  AgaveClient
  (listSystems [this]
    (v2/check-access-token token-info-ref timeout)
    (v2/list-systems base-url token-info-ref timeout))
  (listApps [this]
    (v2/check-access-token token-info-ref timeout)
    (v2/list-apps base-url token-info-ref timeout)))

(defn agave-client-v2
  [base-url token-info & {:keys [timeout] :or {timeout 5000}}]
  (AgaveClientV2. base-url (ref token-info) timeout))
