(ns metadactyl.routes.params
  (:require [ring.swagger.schema :as ss]
            [schema.core :as s])
  (:import [java.util UUID]))

(def AppIdPathParam (ss/describe UUID "The App's UUID"))

(def AppCategoryIdPathParam (ss/describe UUID "The App Category's UUID"))

(s/defschema SecuredQueryParams
  {:user                        (ss/describe String "The short version of the username")
   (s/optional-key :email)      (ss/describe String "The user's email address")
   (s/optional-key :first-name) (ss/describe String "The user's first name")
   (s/optional-key :last-name)  (ss/describe String "The user's last name")})

(s/defschema PagingParams
  {(s/optional-key :limit)
   (ss/describe Long
     "Limits the response to X number of results in the listing array.
      See http://www.postgresql.org/docs/9.3/interactive/queries-limit.html")

   (s/optional-key :offset)
   (ss/describe Long
     "Skips the first X number of results in the listing array.
      See http://www.postgresql.org/docs/9.3/interactive/queries-limit.html")

   (s/optional-key :sortField)
   (ss/describe String
     "Sorts the results in the listing array by the field X, before limits and offsets are applied.
      This field can be any one of the simple fields of the listing results, or `average_rating` or
      `user_rating` for ratings sorting.
      See http://www.postgresql.org/docs/9.3/interactive/queries-order.html")

   (s/optional-key :sortDir)
   (ss/describe String
     "Only used when sortField is present. Sorts the results in either ascending (`ASC`) or
      descending (`DESC`) order, before limits and offsets are applied. Defaults to `ASC`.
      See http://www.postgresql.org/docs/9.3/interactive/queries-order.html")})

(s/defschema CategoryListingParams
  (merge SecuredQueryParams
         {(s/optional-key :public)
          (ss/describe Boolean
            "If set to 'true', then only app categories that are in a workspace that is marked as
             public in the database are returned. If set to 'false', then only app categories that
             are in the user's workspace are returned. If not set, then both public and the user's
             private categories are returned.")}))

(s/defschema AppListingParams
  (merge SecuredQueryParams PagingParams))

(s/defschema AppSearchParams
  (merge AppListingParams
         {:search (ss/describe String "The pattern to match in an App's Name or Description.")}))

