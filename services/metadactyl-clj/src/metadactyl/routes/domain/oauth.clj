(ns metadactyl.routes.domain.oauth
  (:use [common-swagger-api.schema :only [describe]])
  (:require [schema.core :as s]))

(s/defschema OAuthCallbackResponse
  {:state_info (describe String "Arbitrary state information required by the UI.")})
