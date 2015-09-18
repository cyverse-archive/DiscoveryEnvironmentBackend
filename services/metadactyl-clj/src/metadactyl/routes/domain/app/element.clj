(ns metadactyl.routes.domain.app.element
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema optional-key]])
  (:import [java.util UUID]))

(defschema DataSource
  {:id          (describe UUID "A UUID that is used to identify the Data Source")
   :name        (describe String "The Data Source's name")
   :description (describe String "The Data Source's description")
   :label       (describe String "The Data Source's label")})

(defschema FileFormat
  {:id                   (describe UUID "A UUID that is used to identify the File Format")
   :name                 (describe String "The File Format's name")
   (optional-key :label) (describe String "The File Format's label")})

(defschema InfoType
  {:id                   (describe UUID "A UUID that is used to identify the Info Type")
   :name                 (describe String "The Info Type's name")
   (optional-key :label) (describe String "The Info Type's label")})

(defschema ParameterType
  {:id                         (describe UUID "A UUID that is used to identify the Parameter Type")
   :name                       (describe String "The Parameter Type's name")
   (optional-key :description) (describe String "The Parameter Type's description")
   :value_type                 (describe String "The Parameter Type's value type name")})

(defschema RuleType
  {:id                         (describe UUID "A UUID that is used to identify the Rule Type")
   :name                       (describe String "The Rule Type's name")
   (optional-key :description) (describe String "The Rule Type's description")
   :rule_description_format    (describe String "The Rule Type's description format")
   :subtype                    (describe String "The Rule Type's subtype")
   :value_types                (describe [String] "The Rule Type's value types")})

(defschema ToolType
  {:id                         (describe UUID "A UUID that is used to identify the Tool Type")
   :name                       (describe String "The Tool Type's name")
   (optional-key :description) (describe String "The Tool Type's description")
   :label                      (describe String "The Tool Type's label")})

(defschema ValueType
  {:id          (describe UUID "A UUID that is used to identify the Value Type")
   :name        (describe String "The Value Type's name")
   :description (describe String "The Value Type's description")})

(defschema DataSourceListing
  {:data_sources (describe [DataSource] "Listing of App File Parameter Data Sources")})

(defschema FileFormatListing
  {:formats (describe [FileFormat] "Listing of App Parameter File Formats")})

(defschema InfoTypeListing
  {:info_types (describe [InfoType] "Listing of Tool Info Types")})

(defschema ParameterTypeListing
  {:parameter_types (describe [ParameterType] "Listing of App Parameter Types")})

(defschema RuleTypeListing
  {:rule_types (describe [RuleType] "Listing of App Parameter Rule Types")})

(defschema ToolTypeListing
  {:tool_types (describe [ToolType] "Listing of App Tool Types")})

(defschema ValueTypeListing
  {:value_types (describe [ValueType] "Listing of App Parameter and Rule Value Types")})
