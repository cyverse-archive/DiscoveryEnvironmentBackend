(ns data-info.services.filesystem.garnish.controllers
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators])
  (:require [cheshire.core :as json]
            [heuristomancer.core :as hm]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [data-info.services.filesystem.garnish.irods :as prods]
            [data-info.util.validators :as valid]))


(def script-types (sort (hm/supported-formats)))


(defn accepted-types
  []
  (conj (set script-types) ""))


(defn add-type
  [req-body params]
  (let [body  (valid/parse-body (slurp req-body))
        type? #(or (string/blank? %1) (contains? (accepted-types) %1))]
    (validate-map params {:user string?})
    (validate-map body {:path string? :type type?})
    (json/generate-string
     (if-not (string/blank? (:type body))
       (prods/add-type (:user params) (:path body) (:type body))
       (prods/unset-types (:user params) (:path body))))))


(defn delete-type
  [params]
  (log/info "(delete-type) request parameters:" params)
  (log/info "(delete-type) request parameters after conversion:" params)
  (log/info "(delete-type) contains accepted type" (contains? (accepted-types) (:type params)))
  (validate-map params {:user string?
                        :type #(contains? (accepted-types) %)
                        :path string?})
  (json/generate-string (prods/delete-type (:user params) (:path params) (:type params))))


(defn get-types
  [params]
  (validate-map params {:path string? :user string?})
  (json/generate-string {:type (prods/get-types (:user params) (:path params))}))


(defn find-typed-paths
  [params]
  (validate-map params {:user string? :type string?})
  (json/generate-string {:paths (prods/find-paths-with-type (:user params) (:type params))}))


(defn get-type-list
  []
  (json/generate-string {:types script-types}))


(defn set-auto-type
  [req-body params]
  (let [body (valid/parse-body (slurp req-body))]
    (log/warn body)
    (validate-map params {:user string?})
    (validate-map body {:path string?})
    (json/generate-string
      (prods/auto-add-type (:user params) (:path body)))))


(defn preview-auto-type
  [params]
  (validate-map params {:user string? :path string?})
  (json/generate-string (prods/preview-auto-type (:user params) (:path params))))
