(ns data-info.routes.domain.common
  (:use [common-swagger-api.schema :only [describe NonBlankString ->optional-param]])
  (:require [heuristomancer.core :as hm]
            [schema.core :as s])
  (:import [java.util UUID]))

(defn get-error-code-block
  [& error-codes]
  (str "\n\n#### Error Codes:\n    " (clojure.string/join "\n    " error-codes)))

(def DataIdPathParam (describe UUID "The data item's UUID"))

(s/defschema Paths
  {:paths (describe [NonBlankString] "A list of IRODS paths")})

(s/defschema OptionalPaths
  (-> Paths
    (->optional-param :paths)))

(def ValidInfoTypesEnum (apply s/enum (hm/supported-formats)))
(def ValidInfoTypesEnumPlusBlank (apply s/enum (conj (hm/supported-formats) "")))
