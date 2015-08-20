(ns data-info.routes.domain.common
  (:use [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(defn get-error-code-block
  [& error-codes]
  (str "\n\n#### Error Codes:\n    " (clojure.string/join "\n    " error-codes)))

(def NonBlankString
  (describe
    (s/both String (s/pred (complement clojure.string/blank?) 'non-blank?))
    "A non-blank string."))

(def DataIdPathParam (describe UUID "The data item's UUID"))

(def SortFieldOptionalKey (s/optional-key :sort-field))
(def SortFieldDocs
  "Sorts the results in the listing array by the given field, before limits and offsets are applied.
   See http://www.postgresql.org/docs/9.3/interactive/queries-order.html")

(s/defschema SecuredQueryParamsRequired
  {:user (describe NonBlankString "The IRODS username of the requesting user")})

(s/defschema Paths
  {:paths (describe [NonBlankString] "A list of IRODS paths")})

(s/defschema PagingParams
  {(s/optional-key :limit)
   (describe (s/both Long (s/pred pos? 'positive-integer?))
     "Limits the response to X number of results in the listing array.
      See http://www.postgresql.org/docs/9.3/interactive/queries-limit.html")

   (s/optional-key :offset)
   (describe (s/both Long (s/pred (partial <= 0) 'non-negative-integer?))
     "Skips the first X number of results in the listing array.
      See http://www.postgresql.org/docs/9.3/interactive/queries-limit.html")

   SortFieldOptionalKey
   (describe String SortFieldDocs)

   (s/optional-key :sort-order)
   (describe (s/enum "ASC" "DESC")
     "Only used when sort-field is present. Sorts the results in either ascending (`ASC`) or
      descending (`DESC`) order, before limits and offsets are applied. Defaults to `ASC`.
      See http://www.postgresql.org/docs/9.3/interactive/queries-order.html")})
