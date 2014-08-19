(ns mescal.agave-de-v2.params)

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

(def ^:private boolean-types
  #{"bool" "boolean" "flag"})

(def enumeration "enumeration")

(defn get-param-type
  [param]
  (let [type     (get-in param [:value :type])
        ontology (get-in param [:semantics :ontology])
        xsd-type (first (filter (partial re-matches #"xs:.*") ontology))
        regex    (get-in param [:value :validator]) ]
    (cond
     (= type "number")    (number-type-for xsd-type)
     (= type "string")    (string-type-for xsd-type)
     (boolean-types type) "Flag"
     (= type enumeration) "TextSelection"
     :else                "Text")))

(defn format-enum-element
  [default-value enum-element]
  (when enum-element
    (let [[enum-value label] (first enum-element)]
      {:display   label
       :id        (name enum-value)
       :isDefault (= (name enum-value) default-value)
       :name      ""
       :value     (name enum-value)})))

(defn find-enum-element
  [enum-value enumeration-list]
  (let [enum-value (keyword enum-value)]
    (first (filter (fn [m] (let [[k _] (first m)] (= k enum-value)))
                   enumeration-list))))

(defn enum-param?
  [param]
  (= enumeration (get-in param [:value :type])))
