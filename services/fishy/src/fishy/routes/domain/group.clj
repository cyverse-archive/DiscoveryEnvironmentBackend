(ns fishy.routes.domain.group
  (:use [compojure.api.sweet :only [describe]])
  (:require [fishy.routes.domain.params :as params]
            [schema.core :as s]))

(s/defschema Group
  {:description       (describe String "A brief description of the group.")
   :display_extension (describe String "The displayable group name extension.")
   :display_name      (describe String "The displayable group name.")
   :extension         (describe String "The internal group name extension.")
   :id_index          (describe String "The sequential ID index number.")
   :name              (describe String "The internal group name.")
   :type              (describe String "The group type name.")
   :id                (describe String "The group ID.")})

(s/defschema GroupList
  {:groups (describe [Group] "The list of groups in the result set.")})
