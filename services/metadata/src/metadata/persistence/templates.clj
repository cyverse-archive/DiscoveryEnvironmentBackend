(ns metadata.persistence.templates
  (:use [korma.core]))

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
