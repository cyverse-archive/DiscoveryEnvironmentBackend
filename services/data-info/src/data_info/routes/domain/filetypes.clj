(ns data-info.routes.domain.filetypes
  (:use [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s]))

(s/defschema TypesList
  {:types (describe [String] "The available file types.")})
