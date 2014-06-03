(ns mescal.agave-de-v2.apps
  (:use [mescal.agave-de-v2.app-listings :only [get-app-name]])
  (:require [mescal.agave-de-v2.constants :as c]
            [mescal.util :as util]))

(defn- get-boolean
  [value default]
  (cond (nil? value)    default
        (string? value) (Boolean/parseBoolean value)
        :else           value))

(defn- format-group
  [name params]
  (when (some :isVisible params)
    {:name       name
     :label      name
     :id         name
     :type       ""
     :properties params}))

(defn- format-input-validator
  [input]
  {:required (get-boolean (get-in input [:value :required]) false)})

(defn- number-type-for
  [xsd-type]
  (cond
   (= xsd-type "xs:decimal")       "Double"
   (= xsd-type "xs:float")         "Double"
   (= xsd-type "xs:double")        "Double"
   (= xsd-type "xs:integer")       "Integer"
   (= xsd-type "xs:long")          "Integer"
   (= xsd-type "xs:int")           "Integer"
   (= xsd-type "xs:short")         "Integer"
   (= xsd-type "xs:byte")          "Integer"
   (= xsd-type "xs:unsignedLong")  "Integer"
   (= xsd-type "xs:unsignedInt")   "Integer"
   (= xsd-type "xs:unsignedShort") "Integer"
   (= xsd-type "xs:unsignedByte")  "Integer"
   :else                           "Double"))

(defn- string-type-for
  [xsd-type]
  (cond
   (= xsd-type "xs:boolean") "Flag"
   :else                     "Text"))

(defn- get-param-type
  [param]
  (let [type     (get-in param [:value :type])
        ontology (get-in param [:semantics :ontology])
        xsd-type (first (filter (partial re-matches #"xs:.*") ontology))
        regex    (get-in param [:value :validator]) ]
    (cond
     (= type "number") (number-type-for xsd-type)
     (= type "string") (string-type-for xsd-type)
     (= type "bool")   "Flag")))

(defn- format-param
  [get-type get-value param]
  {:arguments    []
   :defaultValue (get-value param)
   :description  (get-in param [:details :description])
   :id           (:id param)
   :isVisible    (get-boolean (get-in param [:value :visible]) false)
   :label        (get-in param [:details :label])
   :name         (:id param)
   :order        0
   :required     (get-boolean (get-in param [:value :required]) false)
   :type         (get-type param)
   :validators   []})

(defn- param-formatter
  [get-type get-value]
  (fn [param]
    (format-param get-type get-value param)))

(defn- get-default-param-value
  [param]
  (get-in param [:value :default]))

(defn- input-param-formatter
  []
  (param-formatter (constantly "FileInput") (constantly "")))

(defn- opt-param-formatter
  [& {:keys [get-default] :or {get-default get-default-param-value}}]
  (param-formatter get-param-type get-default))

(defn- output-param-formatter
  [& {:keys [get-default] :or {get-default get-default-param-value}}]
  (param-formatter (constantly "Output") get-default))

(defn format-groups
  [app]
  (remove nil?
          [(format-group "Inputs" (map (input-param-formatter) (:inputs app)))
           (format-group "Parameters" (map (opt-param-formatter) (:parameters app)))
           (format-group "Outputs" (map (output-param-formatter) (:outputs app)))]))

(defn format-app
  [app]
  (let [app-label (get-app-name app)]
    {:id           (:id app)
     :name         app-label
     :label        app-label
     :component_id c/hpc-group-id
     :groups       (format-groups app)}))
