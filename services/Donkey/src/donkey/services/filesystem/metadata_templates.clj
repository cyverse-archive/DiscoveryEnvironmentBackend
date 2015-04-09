(ns donkey.services.filesystem.metadata-templates
  (:use [clojure-commons.core :only [remove-nil-values]]
        [donkey.auth.user-attributes :only [current-user]]
        [donkey.services.filesystem.common-paths]
        [kameleon.queries :only [get-user-id]]
        [kameleon.uuids :only [is-uuid? uuidify]]
        [korma.core]
        [korma.db :only [transaction with-db]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as error-codes]
            [clojure-commons.validators :as common-validators]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.util.db :as db]
            [donkey.util.service :as service]))

(defn- get-metadata-template
  [id]
  (with-db db/de
    (first (select [:metadata_templates :template]
             (join [:users :created_by] {:template.created_by :created_by.id})
             (join [:users :modified_by] {:template.modified_by :modified_by.id})
             (fields :template.id
                     :template.name
                     [:created_by.username :created_by]
                     :created_on
                     [:modified_by.username :modified_by]
                     :modified_on)
             (where {:template.id id})))))

(defn validate-metadata-template-exists
  [id]
  (when-not (get-metadata-template (uuidify id))
    (throw+ {:error_code error-codes/ERR_DOES_NOT_EXIST
             :metadata_template id})))

(defn- list-metadata-templates
  []
  (with-db db/de
    (select :metadata_templates
            (fields :id :name)
            (where {:deleted false}))))

(defn- get-valid-metadata-template
  [id]
  (if-let [template (get-metadata-template id)]
    template
    (service/not-found "metadata template" id)))

(defn- attr-fields
  [query]
  (fields query
          [:attr.id          :id]
          [:attr.name        :name]
          [:attr.description :description]
          [:attr.required    :required]
          [:value_type.name  :type]
          [:created_by.username :created_by]
          :created_on
          [:modified_by.username :modified_by]
          :modified_on))

(defn- list-metadata-template-attributes
  [id]
  (select [:metadata_template_attrs :mta]
          (join [:metadata_attributes :attr] {:mta.attribute_id :attr.id})
          (join [:metadata_value_types :value_type] {:attr.value_type_id :value_type.id})
          (join [:users :created_by] {:attr.created_by :created_by.id})
          (join [:users :modified_by] {:attr.modified_by :modified_by.id})
          (attr-fields)
          (where {:mta.template_id id})
          (order [:mta.display_order])))

(defn- add-attr-synonyms
  [attr]
  (let [{:keys [id]} attr]
    (->> (doall (map :id (select (sqlfn :metadata_attribute_synonyms id))))
         (assoc attr :synonyms))))

(defn- add-attr-enum-values
  [{:keys [id type] :as attr}]
  (if (= "Enum" type)
    (->> (select :metadata_attr_enum_values
           (fields :id
                   :value
                   :is_default)
           (where {:attribute_id id})
           (order :display_order))
         (assoc attr :values))
    attr))

(defn- format-attribute
  [attr]
  (->> attr
    add-attr-synonyms
    add-attr-enum-values))

(defn- view-metadata-template
  [id]
  (with-db db/de
    (let [template (get-valid-metadata-template id)]
      (assoc template
        :attributes (doall (map format-attribute (list-metadata-template-attributes id)))))))

(defn- get-metadata-attribute
  [id]
  (first
   (select [:metadata_attributes :attr]
           (join [:metadata_value_types :value_type] {:attr.value_type_id :value_type.id})
           (join [:users :created_by] {:attr.created_by :created_by.id})
           (join [:users :modified_by] {:attr.modified_by :modified_by.id})
           (attr-fields)
           (where {:attr.id id}))))

(defn- view-metadata-attribute
  [id]
  (with-db db/de
    (if-let [attr (get-metadata-attribute id)]
      (format-attribute attr)
      (service/not-found "metadata attribute" id))))

(defn- add-metadata-attribute-enum-value
  "Adds a Metadata Template Attribute Enum value to the database."
  [attribute-id order value]
  (let [value (-> value
                  (update-in [:id] uuidify)
                  (update-in [:is_default] boolean)
                  (select-keys [:id :value :is_default])
                  (assoc :attribute_id attribute-id
                         :display_order order)
                  remove-nil-values)]
    (insert :metadata_attr_enum_values (values value))))

(defn- get-metadata-value-type-id
  [type]
  (-> (select :metadata_value_types (fields :id) (where {:name type}))
      first
      :id))

(defn- format-attribute-for-save
  [{:keys [type] :as attribute}]
  (-> attribute
      (update-in [:id] uuidify)
      (update-in [:required] boolean)
      (select-keys [:id :name :description :required])
      (assoc :value_type_id (get-metadata-value-type-id type))
      remove-nil-values))

(defn- add-metadata-template-attribute
  "Adds a Metadata Template Attribute and any associated Enum values to the database."
  [user-id template-id order {enum-values :values type :type :as attribute}]
  (let [attribute (-> attribute
                      format-attribute-for-save
                      (assoc :created_by  user-id
                             :modified_by user-id))
        attr-id (:id (insert :metadata_attributes (values attribute)))]
    (insert :metadata_template_attrs (values {:template_id template-id
                                              :attribute_id attr-id
                                              :display_order order}))
    (dorun
      (map-indexed (partial add-metadata-attribute-enum-value attr-id) enum-values))))

(defn- add-metadata-template
  "Adds a Metadata Template and its associated Attributes to the database."
  [{:keys [username]} {:keys [attributes] :as template}]
  (with-db db/de
    (transaction
      (let [user-id (get-user-id username)
            template (-> template
                         (select-keys [:id :name])
                         (update-in [:id] uuidify)
                         (assoc :created_by  user-id
                                :modified_by user-id)
                         remove-nil-values)
            template-id (:id (insert :metadata_templates (values template)))]
        (dorun
          (map-indexed (partial add-metadata-template-attribute user-id template-id) attributes))
        template-id))))

(defn- update-metadata-template-attribute
  "Updates a Metadata Template Attribute and deletes then re-adds any associated Enum values."
  [user-id template-id order {enum-values :values :as attribute}]
  (let [attribute (-> attribute
                      format-attribute-for-save
                      (assoc :modified_by user-id
                             :modified_on (sqlfn now)))
        attr-id (:id attribute)]
    (update :metadata_attributes
      (set-fields (dissoc attribute :id))
      (where {:id attr-id}))
    (insert :metadata_template_attrs (values {:template_id template-id
                                              :attribute_id attr-id
                                              :display_order order}))
    (delete :metadata_attr_enum_values (where {:attribute_id attr-id}))
    (dorun
      (map-indexed (partial add-metadata-attribute-enum-value attr-id) enum-values))))

(defn- edit-metadata-template-attribute
  "Updates a Metadata Template Attribute, or adds it if it does not already exist in the database."
  [user-id template-id order {:keys [id] :as attribute}]
  (let [attr-exists? (and id (get-metadata-attribute (uuidify id)))]
    (if attr-exists?
      (update-metadata-template-attribute user-id template-id order attribute)
      (add-metadata-template-attribute user-id template-id order attribute))))

(defn- delete-orphan-attributes
  "Deletes all metadata_attributes that are not associated with a Metadata Template and are not a
   synonym for another attribute."
  []
  (delete :metadata_attributes
    (where (not (exists (subselect [:metadata_template_attrs :ta]
                          (where {:metadata_attributes.id :ta.attribute_id})))))
    (where (not (exists (subselect [:metadata_attr_synonyms :s]
                          (where {:metadata_attributes.id :s.synonym_id})))))))

(defn- edit-metadata-template
  "Updates a Metadata Template and adds or updates its associated Attributes. Also deletes any
   orphaned Attributes."
  [{:keys [username]} {:keys [id attributes] :as template}]
  (with-db db/de
    (transaction
      (let [user-id (get-user-id username)
            template (-> template
                         (select-keys [:name :deleted])
                         (assoc :modified_by user-id
                                :modified_on (sqlfn now)))]
        (update :metadata_templates (set-fields template) (where {:id id}))
        (delete :metadata_template_attrs (where {:template_id id}))
        (dorun
          (map-indexed (partial edit-metadata-template-attribute user-id id) attributes))
        (delete-orphan-attributes))))
  id)

(defn- delete-metadata-template
  "Sets a Metadata Template's deleted flag to 'true'."
  [{:keys [username]} template-id]
  (with-db db/de
    (update :metadata_templates
      (set-fields {:deleted true
                   :modified_by (get-user-id username)
                   :modified_on (sqlfn now)})
      (where {:id template-id}))))

(defn do-metadata-template-list
  []
  {:metadata_templates (list-metadata-templates)})

(with-pre-hook! #'do-metadata-template-list
  (fn []
    (log-call "do-metadata-template-list")))

(with-post-hook! #'do-metadata-template-list (log-func "do-metadata-template-list"))

(defn do-metadata-template-view
  [id]
  (view-metadata-template (uuidify id)))

(with-pre-hook! #'do-metadata-template-view
  (fn [id]
    (log-call "do-metadata-template-view" id)))

(with-post-hook! #'do-metadata-template-view (log-func "do-metadata-template-view"))

(defn do-metadata-attribute-view
  [id]
  (view-metadata-attribute (uuidify id)))

(with-pre-hook! #'do-metadata-attribute-view
  (fn [id]
    (log-call "do-metadata-attribute-view" id)))

(with-post-hook! #'do-metadata-attribute-view (log-func "do-metadata-attribute-view"))

(defn- validate-metadata-template
  [{:keys [id attributes] :as template}]
  (common-validators/validate-map template {:name string? :attributes sequential?})
  (when (not (nil? id)) (common-validators/validate-field :id id is-uuid?))
  (common-validators/validate-field :attributes attributes (complement empty?))
  (doseq [{:keys [id type values] :as attr} attributes]
    (common-validators/validate-map attr {:name string?
                                          :description string?
                                          :type string?})
    (when (not (nil? id)) (common-validators/validate-field :id id is-uuid?))
    (when (= "Enum" type)
      (common-validators/validate-map attr {:values sequential?})
      (common-validators/validate-field :values values (complement empty?))
      (doseq [val values]
        (common-validators/validate-map val {:value string?})))))

(defn do-metadata-template-add
  [template]
  (->> template
       (add-metadata-template current-user)
       view-metadata-template))

(with-pre-hook! #'do-metadata-template-add
  (fn [body]
    (log-call "do-metadata-template-add")
    (validate-metadata-template body)))

(with-post-hook! #'do-metadata-template-add (log-func "do-metadata-template-add"))

(defn do-metadata-template-edit
  [template-id template]
  (->> (assoc template :id (uuidify template-id))
       (edit-metadata-template current-user)
       view-metadata-template))

(with-pre-hook! #'do-metadata-template-edit
  (fn [template-id template]
    (log-call "do-metadata-template-edit")
    (common-validators/validate-field :template-id template-id is-uuid?)
    (validate-metadata-template-exists template-id)
    (validate-metadata-template template)))

(with-post-hook! #'do-metadata-template-edit (log-func "do-metadata-template-edit"))

(defn do-metadata-template-delete
  [template-id]
  (delete-metadata-template current-user (uuidify template-id))
  nil)

(with-pre-hook! #'do-metadata-template-delete
  (fn [template-id]
    (log-call "do-metadata-template-delete")
    (common-validators/validate-field :template-id template-id is-uuid?)
    (validate-metadata-template-exists template-id)))

(with-post-hook! #'do-metadata-template-delete (log-func "do-metadata-template-delete"))
