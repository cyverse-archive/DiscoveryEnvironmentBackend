(ns metadactyl.routes.params
  (:require [ring.swagger.schema :as ss]
            [schema.core :as s]))

(s/defschema SecuredQueryParams
  {:user                        String
   (s/optional-key :email)      String
   (s/optional-key :first-name) String
   (s/optional-key :last-name)  String})

;; TODO: add param descriptions to swagger docs.
(s/defschema PagingParams
  {(s/optional-key :limit)     Long ;; Limits the response to X number of results in the "templates" array.
                                    ;; See http://www.postgresql.org/docs/9.3/interactive/queries-limit.html
   (s/optional-key :offset)    Long ;; Skips the first X number of results in the "templates" array.
                                    ;; See http://www.postgresql.org/docs/9.3/interactive/queries-limit.html
   (s/optional-key :sortField) String ;; Sorts the results in the "templates" array by the field X, before
                                      ;; limits and offsets are applied. This field can be any one of the
                                      ;; simple fields of the "templates" objects, or `average_rating` or
                                      ;; `user_rating` for ratings sorting
                                      ;; See http://www.postgresql.org/docs/9.3/interactive/queries-order.html
   (s/optional-key :sortDir)   String}) ;; Only used when sortField is present. Sorts the results in either
                                        ;; ascending (`ASC`) or descending (`DESC`) order, before limits and
                                        ;; offsets are applied. Defaults to `ASC`.
                                        ;; See http://www.postgresql.org/docs/9.3/interactive/queries-order.html

(s/defschema CategoryListingParams
  (merge SecuredQueryParams
         {(s/optional-key :public) Boolean}))

(s/defschema AppListingParams
  (merge SecuredQueryParams PagingParams))

(s/defschema AppSearchParams
  (merge AppListingParams
         {:search String}))

