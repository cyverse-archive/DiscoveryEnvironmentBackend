(ns metadata.persistence.templates
  (:use [clojure-commons.core :only [remove-nil-values]]
        [clojure-commons.assertions :only [assert-found]]
        [korma.core]))

(defn- add-deleted-where-clause
  [query hide-deleted?]
  (if hide-deleted?
    (where query {:deleted false})
    query))

(defn list-templates
  ([]
     (list-templates true))
  ([hide-deleted?]
     (select :templates
             (fields :id :name :deleted :created_by :created_on :modified_by :modified_on)
             (add-deleted-where-clause hide-deleted?)
             (order :name))))

(defn- add-attr-synonyms
  [{:keys [id] :as attr}]
  (->> (select (sqlfn :attribute_synonyms id))
       (map :id)
       (doall)
       (assoc attr :synonyms)))

(defn- list-attr-enum-values
  [attr-id]
  (select :attr_enum_values
          (fields :id
                  :value
                  :is_default)
          (where {:attribute_id attr-id})
          (order :display_order)))

(defn- add-attr-enum-values
  [{:keys [id type] :as attr}]
  (if (= "Enum" type)
    (assoc attr :values (list-attr-enum-values id))
    attr))

(defn- format-attribute
  [attr]
  (->> attr
       add-attr-synonyms
       add-attr-enum-values))

(defn- attr-fields
  [query]
  (fields query
          [:attr.id          :id]
          [:attr.name        :name]
          [:attr.description :description]
          [:attr.required    :required]
          [:value_type.name  :type]
          [:attr.created_by  :created_by]
          [:attr.created_on  :created_on]
          [:attr.modified_by :modified_by]
          [:attr.modified_on :modified_on]))

(defn- list-metadata-template-attributes
  [template-id]
  (select [:template_attrs :mta]
          (join [:attributes :attr] {:mta.attribute_id :attr.id})
          (join [:value_types :value_type] {:attr.value_type_id :value_type.id})
          (attr-fields)
          (where {:mta.template_id template-id})
          (order [:mta.display_order])))

(defn- get-metadata-template
  [id]
  (first (select [:templates :template]
                 (fields :id
                         :name
                         :deleted
                         :created_by
                         :created_on
                         :modified_by
                         :modified_on)
                 (where {:id id}))))

(defn view-template
  [id]
  (when-let [template (get-metadata-template id)]
    (->> (list-metadata-template-attributes id)
         (map format-attribute)
         (assoc template :attributes))))

(defn- get-metadata-attribute
  [id]
  (first  (select [:attributes :attr]
                  (join [:value_types :value_type] {:attr.value_type_id :value_type.id})
                  (attr-fields)
                  (where {:attr.id id}))))

(defn view-attribute
  [id]
  (when-let [attr (get-metadata-attribute id)]
    (format-attribute attr)))

(defn- prepare-attr-enum-value-insertion
  [attr-id order enum-value]
  (->> (assoc (select-keys enum-value [:id :value :is_default])
         :attribute_id  attr-id
         :display_order order)
       (remove-nil-values)))

(defn- add-attr-enum-value
  [attr-id order enum-value]
  (insert :attr_enum_values
          (values (prepare-attr-enum-value-insertion attr-id order enum-value))))

(defn- insert-template-attr
  [template-id order attr-id]
  (insert :template_attrs (values {:template_id   template-id
                                   :attribute_id  attr-id
                                   :display_order order})))

(defn- get-value-type-id
  [type-name]
  (:id (first (select :value_types (where {:name type-name})))))

(defn- prepare-attr-insertion
  [user-id {:keys [type] :as attribute}]
  (->> (assoc (select-keys attribute [:id :name :description :required])
         :created_by    user-id
         :modified_by   user-id
         :value_type_id (assert-found (get-value-type-id type) "value type" type))
       (remove-nil-values)))

(defn- insert-attribute
  [user-id attribute]
  (:id (insert :attributes (values (prepare-attr-insertion user-id attribute)))))

(defn- add-template-attribute
  [user-id template-id order {enum-values :values :as attribute}]
  (let [attr-id (insert-attribute user-id attribute)]
    (insert-template-attr template-id order attr-id)
    (dorun (map-indexed (partial add-attr-enum-value attr-id) enum-values))))

(defn- prepare-template-insertion
  [user-id template]
  (->> (assoc (select-keys template [:id :name])
         :created_by  user-id
         :modified_by user-id)
       (remove-nil-values)))

(defn- insert-template
  [user-id template]
  (:id (insert :templates (values (prepare-template-insertion user-id template)))))

(defn add-template
  [user-id {:keys [attributes] :as template}]
  (let [template-id (insert-template user-id template)]
    (dorun (map-indexed (partial add-template-attribute user-id template-id) attributes))
    template-id))
