(ns donkey.services.filesystem.metadata-templates
  (:use [clj-jargon.init :only [with-jargon]]
        [donkey.services.filesystem.common-paths]
        [donkey.util.config]
        [korma.core]
        [korma.db :only [transaction with-db]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as error-codes]
            [clojure-commons.validators :as common-validators]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.persistence.metadata :as persistence]
            [donkey.services.filesystem.uuids :as uuids]
            [donkey.services.filesystem.validators :as validators]
            [donkey.util.db :as db]
            [donkey.util.service :as service])
  (:import [java.util UUID]))

(defn valid-uuid-to-path
  [cm user uuid]
  (let [path (:path (uuids/path-for-uuid cm user uuid))]
    (when-not path
      (throw+ {:error_code error-codes/ERR_DOES_NOT_EXIST
               :data_id uuid}))
    path))

(defn- get-metadata-template
  [id]
  (with-db db/de
    (first (select :metadata_templates (where {:id id})))))

(defn- validate-metadata-template-exists
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

(defn- get-metadata-template-avus
  "Gets a map containing AVUs for the given Metadata Template and the template's ID."
  [user-id data-id template-id]
  (let [avus (persistence/get-avus-for-metadata-template user-id data-id template-id)]
    {:template_id template-id
     :avus (map #(dissoc % :target_type) avus)}))

(defn- metadata-template-list
  "Lists all Metadata Template AVUs for the given user's data item."
  [user-id data-id]
  (let [template-ids (persistence/get-metadata-template-ids user-id data-id)]
    {:user user-id
     :data_id data-id
     :templates (map (comp (partial get-metadata-template-avus user-id data-id) :template_id)
                     template-ids)}))

(defn- metadata-template-avu-list
  "Lists AVUs for the given Metadata Template on the given user's data item."
  [user-id data-id template-id]
  (assoc (get-metadata-template-avus user-id data-id template-id)
    :user user-id
    :data_id data-id))

(defn- format-avu
  "Formats the given AVU for adding or updating.
   If the AVU already exists, the result will contain its ID."
  [user-id data-id avu]
  (let [avu (-> (select-keys avu [:value :unit])
                (assoc
                  :id (when (:id avu) (UUID/fromString (:id avu)))
                  :target_id data-id
                  :owner_id user-id
                  :attribute (:attr avu)))
        existing-avu (persistence/find-existing-metadata-template-avu avu)]
    (if existing-avu
      (assoc avu :id (:id existing-avu))
      avu)))

(defn- set-metadata-template-avus
  "Adds or Updates AVUs associated with a Metadata Template for the given user's data item."
  [user-id data-id template-id {avus :avus}]
  (let [avus (map (partial format-avu user-id data-id) avus)
        existing-avus (filter :id avus)
        new-avus (map #(assoc % :id (UUID/randomUUID)) (remove :id avus))
        avus (concat existing-avus new-avus)]
    (transaction
     (when (seq existing-avus)
       (dorun (map persistence/update-avu existing-avus)))
     (when (seq new-avus)
       (persistence/add-metadata-template-avus new-avus))
     (dorun (persistence/add-template-instances template-id (map :id avus))))
    {:user user-id
     :data_id data-id
     :template_id template-id
     :avus (map #(select-keys % [:id :attribute :value :unit]) avus)}))

(defn- remove-metadata-template-avu
  "Removes the given Metadata Template AVU association for the given user's data item."
  [user-id data-id template-id avu-id]
  (let [avu {:id avu-id
             :target_id data-id
             :owner_id user-id}
        existing-avu (persistence/find-existing-metadata-template-avu avu)]
    (if existing-avu
      (transaction
       (persistence/remove-avu-template-instances template-id [avu-id])
       (persistence/remove-avu avu-id))
      (throw+ {:error_code error-codes/ERR_DOES_NOT_EXIST
               :avu avu})))
  (service/success-response))

(defn- remove-metadata-template-avus
  "Removes AVUs associated with the given Metadata Template for the given user's data item."
  [user-id data-id template-id]
  (let [avu-ids (map :id (persistence/get-avus-for-metadata-template user-id data-id template-id))]
    (transaction
     (persistence/remove-avu-template-instances template-id avu-ids)
     (persistence/remove-avus avu-ids))
    (service/success-response)))

(defn do-metadata-template-avu-list
  "Lists AVUs associated with a Metadata Template for the given user's data item."
  ([{user :user} data-id]
   (metadata-template-list user (UUID/fromString data-id)))

  ([{user :user} data-id template-id]
   (metadata-template-avu-list user
                               (UUID/fromString data-id)
                               (UUID/fromString template-id))))

(with-pre-hook! #'do-metadata-template-avu-list
  (fn [params data-id & [template-id]]
    (log-call "do-metadata-template-avu-list" params data-id template-id)
    (when template-id (validate-metadata-template-exists template-id))
    (with-jargon (jargon-cfg) [cm]
      (common-validators/validate-map params {:user string?})
      (validators/user-exists cm (:user params))
      (let [user (:user params)
            path (valid-uuid-to-path cm user data-id)]
        (validators/path-readable cm user path)))))

(with-post-hook! #'do-metadata-template-avu-list (log-func "do-metadata-template-avu-list"))

(defn do-set-metadata-template-avus
  "Adds or Updates AVUs associated with a Metadata Template for the given user's data item."
  [{username :user} data-id template-id body]
  (set-metadata-template-avus username
                              (UUID/fromString data-id)
                              (UUID/fromString template-id)
                              body))

(with-pre-hook! #'do-set-metadata-template-avus
  (fn [params data-id template-id body]
    (log-call "do-set-metadata-template-avus" params data-id template-id body)
    (validate-metadata-template-exists template-id)
    (common-validators/validate-map body {:avus sequential?})
    (common-validators/validate-map params {:user string?})
    (with-jargon (jargon-cfg) [cm]
      (validators/user-exists cm (:user params))
      (let [user (:user params)
            path (valid-uuid-to-path cm user data-id)]
        (validators/path-readable cm user path)))))

(with-post-hook! #'do-set-metadata-template-avus (log-func "do-set-metadata-template-avus"))

(defn do-remove-metadata-template-avus
  "Removes AVUs associated with a Metadata Template for the given user's data item."
  ([{user :user} data-id template-id]
   (remove-metadata-template-avus user
                                  (UUID/fromString data-id)
                                  (UUID/fromString template-id)))

  ([{user :user} data-id template-id avu-id]
   (remove-metadata-template-avu user
                                 (UUID/fromString data-id)
                                 (UUID/fromString template-id)
                                 (UUID/fromString avu-id))))

(with-pre-hook! #'do-remove-metadata-template-avus
  (fn [params data-id template-id & [avu-id]]
    (log-call "do-remove-metadata-template-avus" params data-id template-id avu-id)
    (validate-metadata-template-exists template-id)
    (common-validators/validate-map params {:user string?})
    (with-jargon (jargon-cfg) [cm]
      (validators/user-exists cm (:user params))
      (let [user (:user params)
            path (valid-uuid-to-path cm user data-id)]
        (validators/path-readable cm user path)))))

(with-post-hook! #'do-remove-metadata-template-avus (log-func "do-remove-metadata-template-avus"))

