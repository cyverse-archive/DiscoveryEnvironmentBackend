(ns iplant_groups.routes.domain.folder
  (:use [common-swagger-api.schema :only [describe ->optional-param]])
  (:require [iplant_groups.routes.domain.params :as params]
            [schema.core :as s]))

(s/defschema BaseFolder
  {:name
   (describe String "The internal folder name.")

   (s/optional-key :description)
   (describe String "A brief description of the folder.")

   (s/optional-key :display_extension)
   (describe String "The displayable folder name extension.")})

(s/defschema Folder
  (assoc BaseFolder
   (s/optional-key :display_name)
   (describe String "The displayable folder name.")

   (s/optional-key :extension)
   (describe String "The internal folder name extension.")

   :id_index
   (describe String "The sequential ID index number.")

   :id
   (describe String "The folder ID.")))

(s/defschema FolderUpdate
  (-> BaseFolder
    (->optional-param :name)))

(s/defschema FolderStub
  (-> Folder
    (->optional-param :name)
    (->optional-param :id)
    (->optional-param :id_index)))

(s/defschema FolderList
  {:folders (describe [Folder] "The list of folders in the result set.")})
