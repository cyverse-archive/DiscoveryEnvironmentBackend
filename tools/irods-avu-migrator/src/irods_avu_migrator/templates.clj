(ns irods-avu-migrator.templates
  (:use [korma.core])
  (:require [irods-avu-migrator.db :as db]
            [kameleon.uuids :as uuids]
            [korma.db :refer [with-db transaction]]))

(def template-attrs (atom {}))

(defn- get-metadata-template-ids
  []
  (map :id
       (select :metadata_templates
               (fields :id)
               (where {:deleted false}))))

(defn- get-template-attrs
  [template-id]
  (map :attr
       (select [:metadata_template_attrs :mta]
               (fields [:attr.name :attr])
               (join [:metadata_attributes :attr] {:mta.attribute_id :attr.id})
               (where {:mta.template_id template-id}))))

(defn- get-item-avus
  [data-id]
  (with-db db/icat
    (select [:r_meta_main :m]
            (fields [:m.meta_id :meta_id]
                    [:meta_attr_name :attribute]
                    [:meta_attr_value :value]
                    [:meta_attr_unit :unit])
            (join [:r_objt_metamap :om] {:om.meta_id :m.meta_id})
            (where {:om.object_id data-id}))))

(defn- get-folder-template-avus
  []
  (with-db db/icat
    (select [:r_meta_main :m]
            (fields [:m.meta_id :meta_id]
                    [:m.meta_attr_value :template_id]
                    [:c.coll_id :data_id])
            (join [:r_objt_metamap :om] {:om.meta_id :m.meta_id})
            (join [:r_coll_main :c] {:c.coll_id :om.object_id})
            (where {:meta_attr_name "ipc-metadata-template"})
            (where (raw "c.coll_id IS NOT NULL")))))

(defn- get-file-template-avus
  []
  (with-db db/icat
    (select [:r_meta_main :m]
            (fields [:m.meta_id :meta_id]
                    [:m.meta_attr_value :template_id]
                    [:d.data_id :data_id])
            (join [:r_objt_metamap :om] {:om.meta_id :m.meta_id})
            (join [:r_data_main :d] {:d.data_id :om.object_id})
            (where {:meta_attr_name "ipc-metadata-template"})
            (where (raw "d.data_id IS NOT NULL")))))

(defn- fmt-icat-avu
  [target-id avu]
  (-> avu
      (dissoc :meta_id)
      (assoc
        :target_id   target-id
        :unit        (when (not= (:unit avu) "ipc-reserved-unit") (:unit avu))
        :created_by  "iplant-admin"
        :modified_by "iplant-admin"
        :target_type (raw "'data'"))))

(defn- add-metadata-avus
  [target-id template-id avus]
  (let [fmt-avu (partial fmt-icat-avu target-id)
        fmt-template-instance (comp (partial hash-map :template_id template-id, :avu_id) :id)]
    (with-db db/metadata
      (transaction
       (insert :avus (values (map fmt-avu avus)))
       (insert :template_instances (values (map fmt-template-instance avus)))))))

(defn- remove-irods-avus
  [data-id avu-ids]
  (println "Removing ICAT object_id" data-id "AVUs:" avu-ids)
  (with-db db/icat
    (delete :r_objt_metamap (where {:object_id data-id
                                    :meta_id [in avu-ids]}))))

(defn- icat->metadata-avu
  [avu]
  [(:attribute avu) (assoc avu :id (uuids/uuid))])

(defn- convert-data-item-template-avus
  [{:keys [template_id meta_id data_id]}]
  (let [avus (into {} (map icat->metadata-avu (get-item-avus data_id)))
        ipc-uuid (:value (avus "ipc_UUID"))
        avus (remove nil? (map #(avus %) (@template-attrs template_id)))]
    (println "Importing AVUs from template" template_id
             (str "(meta_id " meta_id ")")
             "to target" ipc-uuid
             (str "(object_id " data_id ")"))
    (println "AVU meta_ids:" (map :meta_id avus))
    (add-metadata-avus (uuids/uuidify ipc-uuid) (uuids/uuidify template_id) avus)
    (remove-irods-avus data_id (conj (map :meta_id avus) meta_id))))

(defn- template-id->template-attrs
  [template-id]
  [(str template-id) (get-template-attrs template-id)])

(defn- load-template-attrs
  []
  (reset! template-attrs (into {} (map template-id->template-attrs (get-metadata-template-ids)))))

(defn convert-template-avus
  [options]
  (let [template-avus (concat (get-folder-template-avus) (get-file-template-avus))]
    (load-template-attrs)
    (dorun (map convert-data-item-template-avus template-avus))))
