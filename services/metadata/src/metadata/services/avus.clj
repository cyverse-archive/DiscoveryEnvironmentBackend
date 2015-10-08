(ns metadata.services.avus
  (:use [kameleon.uuids :only [uuid]]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.assertions :as assertions]
            [metadata.persistence.avu :as persistence]
            [metadata.services.templates :as templates]))

(defn- find-existing-metadata-template-avu
  "Formats the given AVU for adding or updating.
   If the AVU already exists, the result will contain its ID."
  [data-id avu]
  (let [avu (-> (select-keys avu [:value :unit])
                (assoc
                  :target_id data-id
                  :attribute (:attr avu)))
        existing-avu (persistence/find-existing-metadata-template-avu avu)]
    (if existing-avu
      (assoc avu :id (:id existing-avu))
      avu)))

(defn set-metadata-template-avus
  "Adds or Updates AVUs associated with a Metadata Template for the given user's data item."
  [user-id data-id data-type template-id {avus :avus}]
  (templates/validate-template-exists template-id)
  (let [avus (map (partial find-existing-metadata-template-avu data-id) avus)
        existing-avus (filter :id avus)
        new-avus (map #(assoc % :id (uuid)) (remove :id avus))
        avus (concat existing-avus new-avus)]
    (transaction
     (when (seq existing-avus)
       (dorun (map (partial persistence/update-avu user-id) existing-avus)))
     (when (seq new-avus)
       (persistence/add-metadata-template-avus user-id new-avus data-type))
     (dorun (persistence/set-template-instances data-id template-id (map :id avus))))
    (persistence/metadata-template-avu-list data-id template-id)))

(defn remove-metadata-template-avu
  "Removes the given Metadata Template AVU association for the given user's data item."
  [user-id data-id template-id avu-id]
  (templates/validate-template-exists template-id)
  (log/warn user-id
            "removing AVU"
            avu-id
            "associated with Metadata Template"
            template-id
            "from data item"
            data-id)
  (let [avu {:id avu-id
             :target_id data-id}
        existing-avu (persistence/find-existing-metadata-template-avu avu)]
    (if existing-avu
      (transaction
       (persistence/remove-avu-template-instances template-id [avu-id])
       (persistence/remove-avu avu-id))
      (throw+ {:type :clojure-commons.exception/not-found
               :avu avu})))
  nil)

(defn remove-metadata-template-avus
  "Removes AVUs associated with the given Metadata Template for the given user's data item."
  [user-id data-id template-id]
  (templates/validate-template-exists template-id)
  (log/warn user-id
            "removing AVUs associated with Metadata Template"
            template-id
            "from data item"
            data-id)
  (let [avu-ids (map :id (persistence/get-avus-for-metadata-template data-id template-id))]
    (transaction
     (persistence/remove-avu-template-instances template-id avu-ids)
     (persistence/remove-avus avu-ids))
    nil))

(defn- find-metadata-template-attributes
  "Returns a map containing a list of the AVUs for the given data-id that match the given set of
   attrs, or nil if no matches were found."
  [attrs {:keys [id]}]
  (let [matching-avus (persistence/get-existing-metadata-template-avus-by-attr id attrs)]
    (if-not (empty? matching-avus)
      {:id   id
       :avus (map persistence/format-avu matching-avus)}
      nil)))

(defn- validate-dest-attrs
  "Throws an error if any of the given dest-items already have Metadata Template AVUs set with any
   of the given attrs."
  [dest-items attrs]
  (let [duplicates (remove nil? (map (partial find-metadata-template-attributes attrs) dest-items))]
    (when-not (empty? duplicates)
      (assertions/not-unique nil nil
        {:reason "Some data items already have metadata with some of the given attributes."
         :duplicates duplicates}))))

(defn- copy-template-avus-to-dest-ids
  "Copies all Metadata Template AVUs from templates to the items with the given data-ids."
  [user templates dest-items]
  (transaction
    (doseq [{:keys [id type]} dest-items]
      (doseq [{template-id :template_id :as template-avus} templates]
        (set-metadata-template-avus user id type template-id template-avus)))))

(defn- get-metadata-template-avu-copies
  "Fetches the list of Metadata Template AVUs for the given data-id, returning only the attr, value,
   and unit in each template's avu list."
  [data-id]
  (let [templates (:templates (persistence/metadata-template-list data-id))
        format-avu-copies (partial map #(select-keys % [:attr :value :unit]))]
    (map #(update % :avus format-avu-copies) templates)))

(defn copy-metadata-template-avus
  "Copies Metadata Template AVUs from the data item with data-id to dest-items. When the 'force?'
   parameter is set, additional validation is performed with the validate-dest-attrs function."
  [user force? data-id {dest-items :filesystem}]
  (let [templates (get-metadata-template-avu-copies data-id)]
    (when-not force?
      (validate-dest-attrs dest-items (set (map :attr (mapcat :avus templates)))))
    (copy-template-avus-to-dest-ids user templates dest-items)
    nil))

(defn list-metadata-template-avus
  "Lists AVUs associated with a Metadata Template for the given user's data item."
  ([data-id]
    (persistence/metadata-template-list data-id))

  ([data-id template-id]
    (templates/validate-template-exists template-id)
    (persistence/metadata-template-avu-list data-id template-id)))
