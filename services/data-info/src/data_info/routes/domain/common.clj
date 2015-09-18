(ns data-info.routes.domain.common
  (:use [common-swagger-api.schema :only [describe NonBlankString]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(defn get-error-code-block
  [& error-codes]
  (str "\n\n#### Error Codes:\n    " (clojure.string/join "\n    " error-codes)))

(def DataIdPathParam (describe UUID "The data item's UUID"))

(s/defschema Paths
  {:paths (describe [NonBlankString] "A list of IRODS paths")})

