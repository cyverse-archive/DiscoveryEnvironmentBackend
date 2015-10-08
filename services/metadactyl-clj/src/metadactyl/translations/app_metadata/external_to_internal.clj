(ns metadactyl.translations.app-metadata.external-to-internal
  (:use [metadactyl.translations.app-metadata.util]
        [slingshot.slingshot :only [throw+]]))

(defn- mark-default-selection-item
  [default-value item]
  (if (and (map? default-value) (= (:id default-value) (:id item)))
    (assoc item :isDefault true)
    (dissoc item :isDefault)))

(defn build-validator-for-property
  "Builds a validator for a property in its external format."
  [{rules :validators required :required args :arguments default-value :defaultValue}]
  (if (or required (seq rules) (seq args))
    (let [rules        (mapv (fn [{:keys [type params]}] {(keyword type) params}) rules)
          mark-default (partial mark-default-selection-item default-value)]
      {:required (true? required)
       :rules    (if (seq args)
                   (conj rules {:MustContain (map mark-default args)})
                   rules)})))

(defn get-default-value
  "Takes a property in its external format and determines what its default value should be
   after it's been translated to its internal format."
  [{prop-type :type args :arguments default-value :defaultValue}]
  (cond (ref-genome-property-types prop-type) (:uuid default-value)
        (seq args)                            nil
        :else                                 default-value))

(defn populate-data-object
  "Populates a data object with information from its parent property."
  [property data-object]
  (let [prop-type     (:type property)
        data-obj-name (if (contains? output-property-types prop-type)
                        (:defaultValue property "")
                        (:label property ""))]
    (when (contains? io-property-types prop-type)
      (assoc data-object
        :cmdSwitch      (:name property "")
        :description    (:description property "")
        :id             (:id property)
        :name           data-obj-name
        :order          (:order property 0)
        :required       (:required property false)
        :file_info_type (data-obj-type-for (:type property) (:file_info_type data-object))
        :multiplicity   (multiplicity-for (:type property) (:multiplicity data-object))))))

(defn translate-property
  "Translates a property from its external format to its internal format."
  [property]
  (assoc (dissoc property
                 :arguments :validators :defaultValue :data_source :file_info_type :format
                 :is_implicit :multiplicity :retain)
    :validator     (build-validator-for-property property)
    :value         (get-default-value property)
    :data_object   (populate-data-object property (:data_object property {}))
    :type          (generic-property-type-for (:type property))
    :omit_if_blank (:omit_if_blank property false)))

(defn translate-property-group
  "Translates a property group from its external format to its internal format."
  [property-group]
  (assoc property-group
    :properties (map translate-property (:properties property-group))))

(defn translate-template
  "Translates a template from its external format to its internal format."
  [template]
  (assoc template
    :groups (map translate-property-group (get-property-groups template))))
