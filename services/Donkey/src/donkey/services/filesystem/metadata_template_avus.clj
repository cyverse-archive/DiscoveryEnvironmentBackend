(ns donkey.services.filesystem.metadata-template-avus
  (:use [clj-jargon.init :only [with-jargon]]
        [donkey.services.filesystem.common-paths]
        [kameleon.uuids :only [uuid uuidify]]
        [korma.core]
        [korma.db :only [transaction with-db]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as error-codes]
            [clojure-commons.validators :as common-validators]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.persistence.metadata :as persistence]
            [donkey.services.filesystem.metadata-templates :as templates]
            [donkey.services.filesystem.uuids :as uuids]
            [donkey.services.filesystem.validators :as validators]
            [donkey.util.db :as db]
            [donkey.util.service :as service]
            [donkey.services.filesystem.icat :as icat]
            [medley.core :as medley]))

(defn- format-avu
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
  (let [avus (persistence/get-avus-for-metadata-template data-id template-id)]
    {:template_id template-id
     :avus (map format-avu avus)}))

(defn- metadata-template-list
  "Lists all Metadata Template AVUs for the given user's data item."
  [data-id]
  (let [template-ids (persistence/get-metadata-template-ids data-id)]
    {:data_id data-id
     :templates (map (comp (partial get-metadata-template-avus data-id) :template_id)
                     template-ids)}))

(defn- metadata-template-avu-list
  "Lists AVUs for the given Metadata Template on the given user's data item."
  [data-id template-id]
  (assoc (get-metadata-template-avus data-id template-id)
    :data_id data-id))

(defn- find-existing-metadata-template-avu
  "Formats the given AVU for adding or updating.
   If the AVU already exists, the result will contain its ID."
  [data-id avu]
  (let [avu (-> (select-keys avu [:value :unit])
                (assoc
                  :id (when (:id avu) (uuidify (:id avu)))
                  :target_id data-id
                  :attribute (:attr avu)))
        existing-avu (persistence/find-existing-metadata-template-avu avu)]
    (if existing-avu
      (assoc avu :id (:id existing-avu))
      avu)))

(defn- set-metadata-template-avus
  "Adds or Updates AVUs associated with a Metadata Template for the given user's data item."
  [user-id data-id template-id {avus :avus}]
  (let [avus (map (partial find-existing-metadata-template-avu data-id) avus)
        existing-avus (filter :id avus)
        new-avus (map #(assoc % :id (uuid)) (remove :id avus))
        avus (concat existing-avus new-avus)
        filter-avu-keys #(select-keys % [:id :attr :value :unit])]
    (transaction
     (when (seq existing-avus)
       (dorun (map (partial persistence/update-avu user-id) existing-avus)))
     (when (seq new-avus)
       (with-jargon (icat/jargon-cfg) [fs]
         (persistence/add-metadata-template-avus user-id
                                                 new-avus
                                                 (icat/resolve-data-type fs data-id))))
     (dorun (persistence/set-template-instances data-id template-id (map :id avus))))
    {:data_id data-id
     :template_id template-id
     :avus (map (comp filter-avu-keys format-avu) avus)}))

(defn- remove-metadata-template-avu
  "Removes the given Metadata Template AVU association for the given user's data item."
  [user-id data-id template-id avu-id]
  (let [avu {:id avu-id
             :target_id data-id}
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
  (let [avu-ids (map :id (persistence/get-avus-for-metadata-template data-id template-id))]
    (transaction
     (persistence/remove-avu-template-instances template-id avu-ids)
     (persistence/remove-avus avu-ids))
    (service/success-response)))

(defn find-metadata-template-attributes
  "Returns a map containing a list of the AVUs for the given data-id that match the given set of
   attrs, or nil if no matches were found."
  [cm user attrs data-id]
  (let [matching-avus (persistence/get-existing-metadata-template-avus-by-attr data-id attrs)]
    (if-not (empty? matching-avus)
      {:id   data-id
       :path (:path (uuids/path-for-uuid cm user data-id))
       :avus (map format-avu matching-avus)}
      nil)))

(defn- validate-dest-attrs
  "Throws an error if any of the given dest-ids already have Metadata Template AVUs set with any of
   the given attrs."
  [user dest-ids attrs]
  (with-jargon (icat/jargon-cfg) [cm]
    (let [duplicates (remove nil? (map (partial find-metadata-template-attributes cm user attrs) dest-ids))]
      (when-not (empty? duplicates)
        (validators/duplicate-attrs-error duplicates)))))

(defn copy-template-avus-to-dest-ids
  "Copies all Metadata Template AVUs from templates to the items with the given data-ids."
  [user templates dest-ids]
  (transaction
    (doseq [data-id dest-ids]
      (doseq [{template-id :template_id :as template-avus} templates]
        (set-metadata-template-avus user data-id template-id template-avus)))))

(defn get-metadata-template-avu-copies
  "Fetches the list of Metadata Template AVUs for the given data-id, returning only the attr, value,
   and unit in each template's avu list."
  [data-id]
  (let [templates (:templates (metadata-template-list data-id))
        format-avu-copies (partial map #(select-keys % [:attr :value :unit]))]
    (map #(medley/update % :avus format-avu-copies) templates)))

(defn- copy-metadata-template-avus
  "Copies all Metadata Template AVUs from the data item with data-id to dest-ids. When the 'force?'
   parameter is set, additional validation is performed with the validate-dest-attrs function."
  [user force? data-id dest-ids]
  (let [templates (get-metadata-template-avu-copies data-id)]
    (if-not force?
      (validate-dest-attrs user dest-ids (set (map :attr (mapcat :avus templates)))))
    (copy-template-avus-to-dest-ids user templates dest-ids)))

(defn do-metadata-template-avu-list
  "Lists AVUs associated with a Metadata Template for the given user's data item."
  ([params data-id]
   (metadata-template-list (uuidify data-id)))

  ([params data-id template-id]
   (metadata-template-avu-list (uuidify data-id)
                               (uuidify template-id))))

(with-pre-hook! #'do-metadata-template-avu-list
  (fn [params data-id & [template-id]]
    (log-call "do-metadata-template-avu-list" params data-id template-id)
    (when template-id (templates/validate-metadata-template-exists template-id))
    (with-jargon (icat/jargon-cfg) [cm]
      (common-validators/validate-map params {:user string?})
      (validators/user-exists cm (:user params))
      (let [user (:user params)
            path (:path (uuids/path-for-uuid cm user data-id))]
        (validators/path-readable cm user path)))))

(with-post-hook! #'do-metadata-template-avu-list (log-func "do-metadata-template-avu-list"))

(defn do-set-metadata-template-avus
  "Adds or Updates AVUs associated with a Metadata Template for the given user's data item."
  [{username :user} data-id template-id body]
  (set-metadata-template-avus username
                              (uuidify data-id)
                              (uuidify template-id)
                              body))

(with-pre-hook! #'do-set-metadata-template-avus
  (fn [params data-id template-id body]
    (log-call "do-set-metadata-template-avus" params data-id template-id body)
    (templates/validate-metadata-template-exists template-id)
    (common-validators/validate-map body {:avus sequential?})
    (common-validators/validate-map params {:user string?})
    (with-jargon (icat/jargon-cfg) [cm]
      (validators/user-exists cm (:user params))
      (let [user (:user params)
            path (:path (uuids/path-for-uuid cm user data-id))]
        (validators/path-writeable cm user path)))))

(with-post-hook! #'do-set-metadata-template-avus (log-func "do-set-metadata-template-avus"))

(defn do-remove-metadata-template-avus
  "Removes AVUs associated with a Metadata Template for the given user's data item."
  ([{user :user} data-id template-id]
   (remove-metadata-template-avus user
                                  (uuidify data-id)
                                  (uuidify template-id)))

  ([{user :user} data-id template-id avu-id]
   (remove-metadata-template-avu user
                                 (uuidify data-id)
                                 (uuidify template-id)
                                 (uuidify avu-id))))

(with-pre-hook! #'do-remove-metadata-template-avus
  (fn [params data-id template-id & [avu-id]]
    (log-call "do-remove-metadata-template-avus" params data-id template-id avu-id)
    (templates/validate-metadata-template-exists template-id)
    (common-validators/validate-map params {:user string?})
    (with-jargon (icat/jargon-cfg) [cm]
      (validators/user-exists cm (:user params))
      (let [user (:user params)
            path (:path (uuids/path-for-uuid cm user data-id))]
        (validators/path-writeable cm user path)))))

(with-post-hook! #'do-remove-metadata-template-avus (log-func "do-remove-metadata-template-avus"))

(defn do-copy-metadata-template-avus
  "Copies Metadata Template AVUs from the given user's data item to other data items."
  [{:keys [user]} data-id force {dest-ids :destination_ids}]
  (copy-metadata-template-avus user force (uuidify data-id) (map uuidify dest-ids)))

(with-pre-hook! #'do-copy-metadata-template-avus
  (fn [{:keys [user] :as params} data-id force {dest-ids :destination_ids :as body}]
    (log-call "do-copy-metadata-template-avus" params data-id body)
    (common-validators/validate-map body {:destination_ids sequential?})
    (common-validators/validate-map params {:user string?})
    (with-jargon (icat/jargon-cfg) [cm]
      (validators/user-exists cm user)
      (let [src-path (:path (uuids/path-for-uuid cm user data-id))
            dest-paths (map (comp :path (partial uuids/path-for-uuid cm user)) dest-ids)]
        (validators/path-readable cm user src-path)
        (validators/all-paths-writeable cm user dest-paths)))))

(with-post-hook! #'do-copy-metadata-template-avus (log-func "do-copy-metadata-template-avus"))
