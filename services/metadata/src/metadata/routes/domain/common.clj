(ns metadata.routes.domain.common
  (:use [clojure.string :only [blank?]]
        [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s]))

(def NonBlankString
  (describe (s/both String (s/pred (complement blank?) 'non-blank-string?)) "A non-blank string."))
