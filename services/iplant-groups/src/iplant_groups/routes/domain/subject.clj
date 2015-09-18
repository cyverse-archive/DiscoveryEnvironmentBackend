(ns iplant_groups.routes.domain.subject
  (:use [common-swagger-api.schema :only [describe]])
  (:require [iplant_groups.routes.domain.params :as params]
            [schema.core :as s]))

(s/defschema Subject
  {:id
   (describe String "The subject ID.")

   (s/optional-key :name)
   (describe String "The subject name.")

   (s/optional-key :first_name)
   (describe String "The subject's first name.")

   (s/optional-key :last_name)
   (describe String "The subject's last name.")

   (s/optional-key :email)
   (describe String "The subject email.")

   (s/optional-key :institution)
   (describe String "The subject institution.")

   (s/optional-key :attribute_values)
   (describe [String] "A list of additional attributes applied to the subject.")

   :source_id
   (describe String "The ID of the source of the subject information.")})

(s/defschema SubjectList
  {:subjects (describe [Subject] "The list of subjects in the result set.")})
