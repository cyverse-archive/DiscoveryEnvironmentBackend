(ns metadactyl.routes.params
  (:require [ring.swagger.schema :as ss]
            [schema.core :as s]))

(s/defschema SecuredQueryParams
  {:user                        String
   (s/optional-key :email)      String
   (s/optional-key :first-name) String
   (s/optional-key :last-name)  String})

(s/defschema CategoryListingParams
  (merge SecuredQueryParams
         {:public Boolean}))

