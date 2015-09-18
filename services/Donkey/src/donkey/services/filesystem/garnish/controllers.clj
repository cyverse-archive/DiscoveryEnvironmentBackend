(ns donkey.services.filesystem.garnish.controllers
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.util.service :only [success-response]]
        [donkey.util.transformers :only [add-current-user-to-map]])
  (:require [cheshire.core :as json]
            [heuristomancer.core :as hm]
            [clojure.core.memoize :as memo]
            [clojure.java.shell :as sh]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [donkey.services.filesystem.garnish.irods :as prods]
            [donkey.util.validators :as valid]))


(def script-types (sort (hm/supported-formats)))

(defn accepted-types
  []
  (conj (set script-types) ""))

(defn add-type
  [req-body req-params]
  (let [body   (valid/parse-body (slurp req-body))
        params (add-current-user-to-map req-params)
        type?  #(or (string/blank? %1) (contains? (accepted-types) %1))]
    (validate-map params {:user string?})
    (validate-map body {:path string? :type type?})
    (success-response
     (if-not (string/blank? (:type body))
       (prods/add-type (:user params) (:path body) (:type body))
       (prods/unset-types (:user params) (:path body))))))
