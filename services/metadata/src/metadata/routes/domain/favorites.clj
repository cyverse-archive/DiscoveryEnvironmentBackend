(ns metadata.routes.domain.favorites
  (:use [compojure.api.sweet :only [describe]]
        [metadata.routes.domain.common])
  (:require [schema.core :as s]))

(s/defschema FavoritesDataListingParams
  (assoc StandardQueryParams
    :entity-type (describe (s/enum "any" "file" "folder") "The type of the requested data items.")))
