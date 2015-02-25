(ns metadactyl.routes.domain.oauth
  (:require [ring.swagger.schema :as ss]
            [schema.core :as s]))

(s/defschema OAuthCallbackResponse
  {:state_info (ss/describe String "Arbitrary state information required by the UI.")})
