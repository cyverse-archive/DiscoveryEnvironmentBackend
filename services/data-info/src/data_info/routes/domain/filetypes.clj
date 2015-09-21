(ns data-info.routes.domain.filetypes
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [data-info.routes.domain.common])
  (:require [schema.core :as s]))

(s/defschema TypesList
  {:types (describe [String] "The available file types.")})

(s/defschema FileType
  {:type (describe ValidInfoTypesEnumPlusBlank "The file's type.")})

(s/defschema FileTypeReturn
  (assoc FileType
         :user (describe NonBlankString "The user performing the request.")
         :path (describe NonBlankString "The iRODS path to the file.")))
