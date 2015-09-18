(ns metadata.persistence.avu
  (:use [korma.core :exclude [update]]
        [korma.db :only [transaction]])
  (:require [kameleon.db :as db]
            [korma.core :as sql]))

(def ^:private data-types [(db/->enum-val "file") (db/->enum-val "folder")])

(defn get-existing-metadata-template-avus-by-attr
  "Finds all existing AVUs by the given data-id and the given set of attributes."
  [data-id attributes]
  (select :avus
    (where {:attribute   [in attributes]
            :target_id   data-id
            :target_type [in data-types]})))

(defn find-existing-metadata-template-avu
  "Finds an existing AVU by ID or attribute, and by target_id."
  [avu]
  (let [id-key (if (:id avu) :id :attribute)]
    (first
      (select :avus
        (where {id-key       (id-key avu)
                :target_id   (:target_id avu)
                :target_type [in data-types]})))))

(defn get-avus-for-metadata-template
  "Gets AVUs for the given Metadata Template."
  [data-id template-id]
  (select :avus
    (join [:template_instances :t]
          {:t.avu_id :avus.id})
    (where {:t.template_id template-id
            :avus.target_id data-id
            :avus.target_type [in data-types]})))

(defn get-metadata-template-ids
  "Finds Metadata Template IDs associated with the given user's data item."
  [data-id]
  (select [:template_instances :t]
    (fields :template_id)
    (join :avus {:t.avu_id :avus.id})
    (where {:avus.target_id data-id
            :avus.target_type [in data-types]})
    (group :template_id)))

(defn remove-avu-template-instances
  "Removes the given Metadata Template AVU associations."
  [template-id avu-ids]
  (delete :template_instances
    (where {:template_id template-id
            :avu_id [in avu-ids]})))

(defn add-template-instances
  "Associates the given AVU with the given Metadata Template ID."
  [template-id avu-ids]
  (transaction
    (remove-avu-template-instances template-id avu-ids)
    (insert :template_instances
      (values (map #(hash-map :template_id template-id, :avu_id %) avu-ids)))))

(defn add-metadata-template-avus
  "Adds the given AVUs to the Metadata database."
  [user-id avus target-type]
  (let [fmt-avu #(assoc %
                   :created_by user-id
                   :modified_by user-id
                   :target_type (db/->enum-val target-type))]
    (insert :avus (values (map fmt-avu avus)))))

(defn update-avu
  "Updates the attribute, value, unit, modified_by, and modified_on fields of the given AVU."
  [user-id avu]
  (sql/update :avus
    (set-fields (-> (select-keys avu [:attribute :value :unit])
                    (assoc :modified_by user-id
                           :modified_on (sqlfn now))))
    (where (select-keys avu [:id]))))

(defn remove-avus
  "Removes AVUs with the given IDs from the Metadata database."
  [avu-ids]
  (delete :avus (where {:id [in avu-ids]})))

(defn remove-avu
  "Removes the AVU with the given ID from the Metadata database."
  [avu-id]
  (delete :avus (where {:id avu-id})))

(defn remove-data-item-template-instances
  "Removes all Metadata Template AVU associations from the given data item."
  [data-id]
  (let [avu-id-select (-> (select* :avus)
                          (fields :id)
                          (where {:target_id data-id
                                  :target_type [in data-types]}))]
    (delete :template_instances (where {:avu_id [in (subselect avu-id-select)]}))))

(defn set-template-instances
  "Associates the given AVU IDs with the given Metadata Template ID,
   removing all other Metadata Template ID associations."
  [data-id template-id avu-ids]
  (transaction
    (remove-data-item-template-instances data-id)
    (add-template-instances template-id avu-ids)))

(defn format-avu
  "Formats a Metadata Template AVU for JSON responses."
  [avu]
  (let [convert-timestamp #(assoc %1 %2 (db/millis-from-timestamp (%2 %1)))]
    (-> avu
      (convert-timestamp :created_on)
      (convert-timestamp :modified_on)
      (assoc :attr (:attribute avu))
      (dissoc :attribute :target_type))))

(defn- get-metadata-template-avus
  "Gets a map containing AVUs for the given Metadata Template and the template's ID."
  [data-id template-id]
  (let [avus (get-avus-for-metadata-template data-id template-id)]
    {:template_id template-id
     :avus (map format-avu avus)}))

(defn metadata-template-list
  "Lists all Metadata Template AVUs for the given user's data item."
  [data-id]
  (let [template-ids (get-metadata-template-ids data-id)]
    {:data_id data-id
     :templates (map (comp (partial get-metadata-template-avus data-id) :template_id)
                  template-ids)}))

(defn metadata-template-avu-list
  "Lists AVUs for the given Metadata Template on the given user's data item."
  [data-id template-id]
  (assoc (get-metadata-template-avus data-id template-id)
    :data_id data-id))
