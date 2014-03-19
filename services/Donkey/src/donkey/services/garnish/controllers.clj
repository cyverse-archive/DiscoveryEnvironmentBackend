(ns donkey.services.garnish.controllers
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.util.validators]
        [donkey.util.transformers :only [add-current-user-to-map]])
  (:require [cheshire.core :as json]
            [hoot.rdf :as rdf]
            [hoot.csv :as csv]
            [heuristomancer.core :as hm]
            [clojure.core.memoize :as memo]
            [clojure.java.shell :as sh]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [donkey.services.garnish.irods :as prods]
            [donkey.util.config :as cfg]))

(def script-types (sort (hm/supported-formats)))

(defn accepted-types
  []
  (conj (set script-types) ""))

(defn add-type
  [req-body req-params]
  (let [body   (parse-body (slurp req-body))
        params (add-current-user-to-map req-params)
        type?  #(or (string/blank? %1) (contains? (accepted-types) %1))]
    (validate-map params {:user string?})
    (validate-map body {:path string? :type type?})
    (json/generate-string
     (if-not (string/blank? (:type body))
       (prods/add-type (:user params) (:path body) (:type body))
       (prods/unset-types (:user params) (:path body))))))

(defn delete-type
  [req-params]
  (log/info "(delete-type) request parameters:" req-params)
  (let [params (add-current-user-to-map req-params)]
    (log/info "(delete-type) request parameters after conversion:" params)
    (log/info "(delete-type) contains accepted type" (contains? (accepted-types) (:type params)))
    (validate-map params {:user string?
                          :type #(contains? (accepted-types) %)
                          :path string?})
    (json/generate-string
      (prods/delete-type (:user params) (:path params) (:type params)))))

(defn get-types
  [req-params]
  (let [params (add-current-user-to-map req-params)]
    (validate-map params {:path string?
                          :user string?})
    (json/generate-string
      {:type (prods/get-types (:user params) (:path params))})))

(defn find-typed-paths
  [req-params]
  (let [params (add-current-user-to-map req-params)]
    (validate-map params {:user string? :type string?})
    (json/generate-string
      {:paths (prods/find-paths-with-type (:user params) (:type params))})))

(defn get-type-list
  []
  (json/generate-string {:types script-types}))

(defn set-auto-type
  [req-body req-params]
  (let [body   (parse-body (slurp req-body))
        params (add-current-user-to-map req-params)]
    (log/warn body)
    (validate-map params {:user string?})
    (validate-map body {:path string?})
    (json/generate-string
      (prods/auto-add-type (:user params) (:path body)))))

(defn preview-auto-type
  [req-params]
  (let [params (add-current-user-to-map req-params)]
    (validate-map params {:user string? :path string?})
    (json/generate-string
      (prods/preview-auto-type (:user params) (:path params)))))
