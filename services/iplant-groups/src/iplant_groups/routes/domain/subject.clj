(ns iplant_groups.routes.domain.subject
  (:use [compojure.api.sweet :only [describe]])
  (:require [iplant_groups.routes.domain.params :as params]
            [schema.core :as s]))

(s/defschema Subject
  {(s/optional-key :attribute_values)
   (describe [String] "A list of attributes applied to the subject.")

   :id
   (describe String "The subject ID.")

   (s/optional-key :name)
   (describe String "The subject name.")

   :source_id
   (describe String "The ID of the source of the subject information.")})

(s/defschema SubjectList
  {:subjects (describe [Subject] "The list of subjects in the result set.")})
