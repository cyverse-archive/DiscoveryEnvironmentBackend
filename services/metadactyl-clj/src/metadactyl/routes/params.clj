(ns metadactyl.routes.params
  (:require [ring.swagger.schema :as ss]
            [schema.core :as s])
  (:import [java.util UUID]))

(defn ->optional-param
  "Removes a required param from the given schema and re-adds it as an optional param."
  [schema param]
  (-> schema
    (assoc (s/optional-key param) (schema param))
    (dissoc param)))

(def AppIdPathParam (ss/describe UUID "The App's UUID"))
(def AppCategoryIdPathParam (ss/describe UUID "The App Category's UUID"))

(s/defschema SecuredQueryParamsRequired
  {:user       (ss/describe String "The short version of the username")
   :email      (ss/describe String "The user's email address")
   :first-name (ss/describe String "The user's first name")
   :last-name  (ss/describe String "The user's last name")})

(s/defschema SecuredQueryParamsEmailRequired
  (-> SecuredQueryParamsRequired
      (->optional-param :first-name)
      (->optional-param :last-name)))

(s/defschema SecuredQueryParams
  (-> SecuredQueryParamsEmailRequired
    (->optional-param :email)))

(s/defschema PagingParams
  {(s/optional-key :limit)
   (ss/describe Long
     "Limits the response to X number of results in the listing array.
      See http://www.postgresql.org/docs/9.3/interactive/queries-limit.html")

   (s/optional-key :offset)
   (ss/describe Long
     "Skips the first X number of results in the listing array.
      See http://www.postgresql.org/docs/9.3/interactive/queries-limit.html")

   (s/optional-key :sort-field)
   (ss/describe String
     "Sorts the results in the listing array by the field X, before limits and offsets are applied.
      This field can be any one of the simple fields of the listing results, or `average_rating` or
      `user_rating` for ratings sorting.
      See http://www.postgresql.org/docs/9.3/interactive/queries-order.html")

   (s/optional-key :sort-dir)
   (ss/describe String
     "Only used when sort-field is present. Sorts the results in either ascending (`ASC`) or
      descending (`DESC`) order, before limits and offsets are applied. Defaults to `ASC`.
      See http://www.postgresql.org/docs/9.3/interactive/queries-order.html")})

(s/defschema SecuredPagingParams
  (merge SecuredQueryParams PagingParams))

(s/defschema SecuredPagingParamsEmailRequired
  (merge SecuredQueryParamsEmailRequired PagingParams))

(s/defschema AppSearchParams
  (merge SecuredPagingParams
         {:search (ss/describe String "The pattern to match in an App's Name or Description.")}))

(s/defschema ToolSearchParams
  (merge SecuredPagingParams
    {:search (ss/describe String "The pattern to match in an Tool's Name or Description.")}))

(s/defschema AppParameterTypeParams
  (merge SecuredQueryParams
    {(s/optional-key :tool-type) (ss/describe String "Filters results by tool type")
     (s/optional-key :tool-id)   (ss/describe UUID "Filters results by tool identifier")}))
