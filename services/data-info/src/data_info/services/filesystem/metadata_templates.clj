(ns data-info.services.filesystem.metadata-templates
  (:use [data-info.services.filesystem.common-paths]
        [korma.core]
        [korma.db :only [with-db]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as error-codes]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.util.db :as db]
            [data-info.util.service :as service])
  (:import [java.util UUID]))

(defn- get-metadata-template
  [id]
  (with-db db/de
    (first (select :metadata_templates (where {:id id})))))

(defn validate-metadata-template-exists
  [id]
  (when-not (get-metadata-template (UUID/fromString id))
    (throw+ {:error_code error-codes/ERR_DOES_NOT_EXIST
             :metadata_template id})))

(defn- list-metadata-templates
  []
  (with-db db/de
    (select :metadata_templates
            (fields :id :name)
            (where {:deleted false}))))

(defn- get-metadata-template-name
  [id]
  (if-let [template-name (:name (get-metadata-template id))]
    template-name
    (service/not-found "metadata template" id)))

(defn- attr-fields
  [query]
  (fields query
          [:attr.id          :id]
          [:attr.name        :name]
          [:attr.description :description]
          [:attr.required    :required]
          [:value_type.name  :type]))

(defn- list-metadata-template-attributes
  [id]
  (select [:metadata_template_attrs :mta]
          (join [:metadata_attributes :attr] {:mta.attribute_id :attr.id})
          (join [:metadata_value_types :value_type] {:attr.value_type_id :value_type.id})
          (attr-fields)
          (where {:mta.template_id id})
          (order [:mta.display_order])))

(defn- add-attr-synonyms
  [attr]
  (let [{:keys [id]} attr]
    (->> (doall (map :id (select (sqlfn :metadata_attribute_synonyms id))))
         (assoc attr :synonyms))))

(defn- view-metadata-template
  [id]
  (with-db db/de
    {:id         id
     :name       (get-metadata-template-name id)
     :attributes (doall (map add-attr-synonyms (list-metadata-template-attributes id)))}))

(defn- get-metadata-attribute
  [id]
  (first
   (select [:metadata_attributes :attr]
           (join [:metadata_value_types :value_type] {:attr.value_type_id :value_type.id})
           (attr-fields)
           (where {:attr.id id}))))

(defn- view-metadata-attribute
  [id]
  (with-db db/de
    (if-let [attr (get-metadata-attribute id)]
      (add-attr-synonyms attr)
      (service/not-found "metadata attribute" id))))

(defn do-metadata-template-list
  []
  {:metadata_templates (list-metadata-templates)})

(with-pre-hook! #'do-metadata-template-list
  (fn []
    (log-call "do-metadata-template-list")))

(with-post-hook! #'do-metadata-template-list (log-func "do-metadata-template-list"))

(defn do-metadata-template-view
  [id]
  (view-metadata-template (UUID/fromString id)))

(with-pre-hook! #'do-metadata-template-view
  (fn [id]
    (log-call "do-metadata-template-view" id)))

(with-post-hook! #'do-metadata-template-view (log-func "do-metadata-template-view"))

(defn do-metadata-attribute-view
  [id]
  (view-metadata-attribute (UUID/fromString id)))

(with-pre-hook! #'do-metadata-attribute-view
  (fn [id]
    (log-call "do-metadata-attribute-view" id)))

(with-post-hook! #'do-metadata-attribute-view (log-func "do-metadata-attribute-view"))

