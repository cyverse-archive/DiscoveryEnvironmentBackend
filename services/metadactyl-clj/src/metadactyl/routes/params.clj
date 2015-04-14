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
(def AppIdJobViewPathParam (ss/describe String "The App's ID"))
(def AppCategoryIdPathParam (ss/describe UUID "The App Category's UUID"))

(def ApiName (ss/describe String "The name of the external API"))

(def AnalysisIdPathParam (ss/describe UUID "The Analysis UUID"))

(def NonBlankString
  (ss/describe
   (s/both String (s/pred (complement clojure.string/blank?) 'non-blank?))
   "A non-blank string."))

(s/defschema SecuredQueryParamsRequired
  {:user       (ss/describe NonBlankString "The short version of the username")
   :email      (ss/describe NonBlankString "The user's email address")
   :first-name (ss/describe NonBlankString "The user's first name")
   :last-name  (ss/describe NonBlankString "The user's last name")})

(s/defschema SecuredQueryParamsEmailRequired
  (-> SecuredQueryParamsRequired
      (->optional-param :first-name)
      (->optional-param :last-name)))

(s/defschema SecuredQueryParams
  (-> SecuredQueryParamsEmailRequired
    (->optional-param :email)))

(s/defschema OAuthCallbackQueryParams
  (assoc SecuredQueryParams
    :code  (ss/describe NonBlankString "The authorization code used to obtain the access token.")
    :state (ss/describe NonBlankString "The authorization state information.")))

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

(s/defschema IncludeHiddenParams
  {(s/optional-key :include-hidden)
   (ss/describe Boolean "True if hidden elements should be included in the results.")})

(s/defschema SecuredPagingParams
  (merge SecuredQueryParams PagingParams))

(s/defschema SecuredPagingParamsEmailRequired
  (merge SecuredQueryParamsEmailRequired PagingParams))

(s/defschema AppSearchParams
  (merge SecuredPagingParams
         {:search (ss/describe String "The pattern to match in an App's Name or Description.")}))

(s/defschema SecuredIncludeHiddenParams
  (merge SecuredQueryParams IncludeHiddenParams))

(s/defschema SecuredIncludeHiddenPagingParams
  (merge SecuredPagingParams IncludeHiddenParams))

(s/defschema ToolSearchParams
  (merge SecuredPagingParams IncludeHiddenParams
    {:search (ss/describe String "The pattern to match in an Tool's Name or Description.")}))

(s/defschema AppParameterTypeParams
  (merge SecuredQueryParams
    {(s/optional-key :tool-type) (ss/describe String "Filters results by tool type")
     (s/optional-key :tool-id)   (ss/describe UUID "Filters results by tool identifier")}))
