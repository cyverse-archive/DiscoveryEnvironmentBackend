(ns metadactyl.routes.params
  (:use [common-swagger-api.schema :only [->optional-param
                                          describe
                                          NonBlankString
                                          PagingParams
                                          StandardUserQueryParams]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def AppIdPathParam (describe UUID "The App's UUID"))
(def AppIdJobViewPathParam (describe String "The App's ID"))
(def AppCategoryIdPathParam (describe UUID "The App Category's UUID"))
(def ToolIdParam (describe UUID "A UUID that is used to identify the Tool"))

(def ApiName (describe String "The name of the external API"))

(def AnalysisIdPathParam (describe UUID "The Analysis UUID"))

(def ResultsTotalParam
  (describe Long
    "The total number of results that would be returned without limits and offsets applied."))

(s/defschema SecuredQueryParamsRequired
  (merge StandardUserQueryParams
    {:email      (describe NonBlankString "The user's email address")
     :first-name (describe NonBlankString "The user's first name")
     :last-name  (describe NonBlankString "The user's last name")}))

(s/defschema SecuredQueryParamsEmailRequired
  (-> SecuredQueryParamsRequired
      (->optional-param :first-name)
      (->optional-param :last-name)))

(s/defschema SecuredQueryParams
  (-> SecuredQueryParamsEmailRequired
    (->optional-param :email)))

(s/defschema OAuthCallbackQueryParams
  (assoc SecuredQueryParams
    :code  (describe NonBlankString "The authorization code used to obtain the access token.")
    :state (describe NonBlankString "The authorization state information.")))

(s/defschema IncludeHiddenParams
  {(s/optional-key :include-hidden)
   (describe Boolean "True if hidden elements should be included in the results.")})

(s/defschema SecuredPagingParams
  (merge SecuredQueryParams PagingParams))

(s/defschema AppSearchParams
  (merge SecuredPagingParams
         {:search (describe String "The pattern to match in an App's Name or Description.")}))

(s/defschema SecuredIncludeHiddenParams
  (merge SecuredQueryParams IncludeHiddenParams))

(s/defschema FilterParams
  {:field
   (describe String "The name of the field on which the filter is based.")

   :value
   (describe (s/maybe String)
     "The search value. If `field` is `name` or `app_name`, then `value` can be contained anywhere,
      case-insensitive, in the corresponding field.")})

;; JSON query params are not currently supported by compojure-api,
;; so we have to define "filter" in this schema as a String for now.
(def OptionalKeyFilter (s/optional-key :filter))

(s/defschema SecuredAnalysisListingParams
  (merge SecuredPagingParams IncludeHiddenParams
    {OptionalKeyFilter
     (describe String
       "Allows results to be filtered based on the value of some result field.
        The format of this parameter is
        `[{\"field\":\"some_field\", \"value\":\"search-term\"}, ...]`, where `field` is the name of
        the field on which the filter is based and `value` is the search value.
        If `field` is `name` or `app_name`, then `value` can be contained anywhere,
        case-insensitive, in the corresponding field.
        For example, to obtain the list of all jobs that were executed using an application with
        `CACE` anywhere in its name, the parameter value can be
        `[{\"field\":\"app_name\",\"value\":\"cace\"}]`.
        To find a job with a specific `id`, the parameter value can be
        `[{\"field\":\"id\",\"value\":\"C09F5907-B2A2-4429-A11E-5B96F421C3C1\"}]`.
        To find jobs associated with a specific `parent_id`, the parameter value can be
        `[{\"field\":\"parent_id\",\"value\":\"b4c2f624-7cbd-496e-adad-5be8d0d3b941\"}]`.
        It's also possible to search for jobs without a parent using this parameter value:
        `[{\"field\":\"parent_id\",\"value\":null}]`.")}))

(s/defschema ToolSearchParams
  (merge SecuredPagingParams IncludeHiddenParams
    {:search (describe String "The pattern to match in an Tool's Name or Description.")}))

(s/defschema AppParameterTypeParams
  (merge SecuredQueryParams
    {(s/optional-key :tool-type) (describe String "Filters results by tool type")
     (s/optional-key :tool-id)   (describe UUID "Filters results by tool identifier")}))
