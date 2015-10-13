(ns common-swagger-api.schema
  (:use [clojure.string :only [blank?]]
        [potemkin :only [import-vars]])
  (:require compojure.api.sweet
            [ring.swagger.json-schema :as json-schema]
            [schema.core :as s]))

(import-vars
  [compojure.api.sweet
   api
   defapi
   middlewares

   describe

   swagger-ui
   swagger-docs
   swaggered

   defroutes*
   context*

   GET*
   ANY*
   HEAD*
   PATCH*
   DELETE*
   OPTIONS*
   POST*
   PUT*])

;; extend schema/Any so that these params still display in the swagger docs.
(extend-type schema.core.AnythingSchema
  json-schema/JsonSchema
  (json-schema/convert [_ _]
    {:type "any"}))

(def ->required-key s/explicit-schema-key)

(defn ->required-param
  "Removes an optional param from the given schema and re-adds it as a required param."
  [schema param]
  (-> schema
    (assoc (->required-key param) (schema param))
    (dissoc param)))

(defn ->optional-param
  "Removes a required param from the given schema and re-adds it as an optional param."
  [schema param]
  (-> schema
    (assoc (s/optional-key param) (schema param))
    (dissoc param)))

(def NonBlankString
  (describe (s/both String (s/pred (complement blank?) 'non-blank-string?)) "A non-blank string."))

(s/defschema StandardUserQueryParams
  {:user (describe NonBlankString "The username of the authenticated, requesting user")})

;; The SortField Docs and OptionalKey are defined seperately so that they can be used to describe
;; different enums in the PagingParams in different endpoints.
(def SortFieldOptionalKey (s/optional-key :sort-field))
(def SortFieldDocs
  "Sorts the results in the listing array by the given field, before limits and offsets are applied.
   See http://www.postgresql.org/docs/9.2/interactive/queries-order.html")

(s/defschema PagingParams
  {(s/optional-key :limit)
   (describe (s/both Long (s/pred pos? 'positive-integer?))
     "Limits the response to X number of results in the listing array.
      See http://www.postgresql.org/docs/9.2/interactive/queries-limit.html")

   (s/optional-key :offset)
   (describe (s/both Long (s/pred (partial <= 0) 'non-negative-integer?))
     "Skips the first X number of results in the listing array.
      See http://www.postgresql.org/docs/9.2/interactive/queries-limit.html")

   ;; SortField is a String by default.
   SortFieldOptionalKey
   (describe String SortFieldDocs)

   (s/optional-key :sort-dir)
   (describe (s/enum "ASC" "DESC")
     "Only used when sort-field is present. Sorts the results in either ascending (`ASC`) or
      descending (`DESC`) order, before limits and offsets are applied. Defaults to `ASC`.
      See http://www.postgresql.org/docs/9.2/interactive/queries-order.html")})
