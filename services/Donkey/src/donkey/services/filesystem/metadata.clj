(ns donkey.services.filesystem.metadata
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.util.config]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.metadata]
        [kameleon.queries :only [get-user-id]]
        [korma.core]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [clojure.data.codec.base64 :as b64]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.persistence.metadata :as persistence]
            [donkey.services.filesystem.validators :as validators]
            [donkey.util.db :as db]
            [donkey.util.service :as service])
  (:import [java.util UUID]))

(defn- fix-unit
  "Used to replace the IPCRESERVED unit with an empty string."
  [avu]
  (if (= (:unit avu) IPCRESERVED)
    (assoc avu :unit "")
    avu))

(def ipc-regex #"(?i)^ipc")

(defn ipc-avu?
  "Returns a truthy value if the AVU map passed in is reserved for the DE's use."
  [avu]
  (or (re-find ipc-regex (:attr avu))
      (re-find ipc-regex (:unit avu))))

(defn authorized-avus
  "Validation to make sure the AVUs aren't system AVUs. Throws a slingshot error
   map if the validation fails."
  [avus]
  (when (some ipc-avu? avus)
    (throw+ {:error_code ERR_NOT_AUTHORIZED
             :avus avus})))

(defn- list-path-metadata
  "Returns the metadata for a path. Passes all AVUs to (fix-unit).
   AVUs with a unit matching IPCSYSTEM are filtered out."
  [cm path]
  (let [ipc-regex #"(?i)^ipc"]
    (filterv
     #(not= (:unit %) IPCSYSTEM)
     (map fix-unit (get-metadata cm (ft/rm-last-slash path))))))

(defn- reserved-unit
  "Turns a blank unit into a reserved unit."
  [avu-map]
  (if (string/blank? (:unit avu-map))
    IPCRESERVED
    (:unit avu-map)))

(defn metadata-get
  "Returns the metadata for a path. Filters out system AVUs and replaces
   units set to ipc-reserved with an empty string."
  [user path]
  (with-jargon (jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-readable cm user path)
    {:metadata (list-path-metadata cm path)}))

(defn- common-metadata-set
  "Adds an AVU to 'path'. The AVU is passed in as a map in the format:
   {
      :attr attr-string
      :value value-string
      :unit unit-string
   }
   It's a no-op if an AVU with the same attribute and value is already
   associated with the path."
  [cm path avu-map]
  (let [fixed-path (ft/rm-last-slash path)
        new-unit   (reserved-unit avu-map)
        attr       (:attr avu-map)
        value      (:value avu-map)]
    (log/warn "Fixed Path:" fixed-path)
    (log/warn "check" (true? (attr-value? cm fixed-path attr value)))
    (when-not (attr-value? cm fixed-path attr value)
      (log/warn "Adding " attr value "to" fixed-path)
      (add-metadata cm fixed-path attr value new-unit))
    fixed-path))

(defn metadata-set
  "Allows user to set metadata on a path. The user must exist in iRODS
   and have write permissions on the path. The path must exist. The
   avu-map parameter must be in this format:
   {
      :attr attr-string
      :value value-string
      :unit unit-string
   }"
  [user path avu-map]
  (with-jargon (jargon-cfg) [cm]
    (validators/user-exists cm user)
    (when (= "failure" (:status avu-map))
      (throw+ {:error_code ERR_INVALID_JSON}))
    (validators/path-exists cm path)
    (validators/path-writeable cm user path)
    {:path (common-metadata-set cm path avu-map)
     :user user}))

(defn admin-metadata-set
  "Adds the AVU to path, bypassing user permission checks. See (metadata-set)
   for the AVU map format."
  [path avu-map]
  (with-jargon (jargon-cfg) [cm]
    (when (= "failure" (:status avu-map))
      (throw+ {:error_code ERR_INVALID_JSON}))
    (validators/path-exists cm path)
    (validators/path-writeable cm (irods-user) path)
    (common-metadata-set cm path avu-map)))

(defn- encode-str
  "Returns str-to-encode as a base 64 encoded string."
  [str-to-encode]
  (String. (b64/encode (.getBytes str-to-encode))))

(defn- workaround-delete
  "Gnarly workaround for a bug (I think) in Jargon. If a value
   in an AVU is formatted a certain way, it can't be deleted.
   We're base64 encoding the value before deletion to ensure
   that the deletion will work."
  [cm path attr value]
  (let [{:keys [attr value unit]} (first (get-attribute-value cm path attr value))
        new-val (encode-str value)]
    (add-metadata cm path attr new-val unit)
    new-val))

(defn- metadata-batch-set
  "Adds and deletes metadata on path for a user. add-dels should be in the
   following format:
   {
      :delete [{:attr :value :unit}]
      :add [{:attr :value :unit}]
   }
   All value in the maps should be strings, just like with (metadata-set)."
  [user path adds-dels]
  (with-jargon (jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-writeable cm user path)
    (let [new-path (ft/rm-last-slash path)]
      (doseq [del (:delete adds-dels)]
        (let [attr  (:attr del)
              value (:value del)]
          (if (attr-value? cm new-path attr value)
            (delete-metadata cm new-path attr value))))
      (doseq [avu (:add adds-dels)]
        (let [new-unit (reserved-unit avu)
              attr     (:attr avu)
              value    (:value avu)]
          (if-not (attr-value? cm new-path attr value)
            (add-metadata cm new-path attr value new-unit))))
      {:path new-path :user user})))

(defn metadata-delete
  "Deletes an AVU from path on behalf of a user. attr and value should be strings."
  [user path attr value]
  (with-jargon (jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-writeable cm user path)
    (delete-metadata cm path attr value)
    {:path path :user user}))

(defn- check-avus
  [adds]
  (mapv
   #(and (map? %1)
         (contains? %1 :attr)
         (contains? %1 :value)
         (contains? %1 :unit))
   adds))

(defn do-metadata-get
  "Entrypoint for the API. Calls (metadata-get). Parameter should be a map
   with :user and :path as keys. Values are strings."
  [{user :user path :path}]
  (metadata-get user path))

(with-pre-hook! #'do-metadata-get
  (fn [params]
    (log-call "do-metadata-get" params)
    (validate-map params {:user string? :path string?})))

(with-post-hook! #'do-metadata-get (log-func "do-metadata-get"))

(defn do-metadata-set
  "Entrypoint for the API. Calls (metadata-set). Parameter should be a map
   with :user and :path as keys. Values are strings."
  [{user :user path :path} body]
  (metadata-set user path body))

(with-pre-hook! #'do-metadata-set
  (fn [params body]
    (log-call "do-metadata-set" params body)
    (validate-map params {:user string? :path string?})
    (validate-map body {:attr string? :value string? :unit string?})))

(with-post-hook! #'do-metadata-set (log-func "do-metadata-set"))

(defn do-metadata-batch-set
  "Entrypoint for the API that calls (metadata-batch-set). Parameter is a map
   with :user and :path as keys. Values are strings."
  [{user :user path :path} body]
  (metadata-batch-set user path body))

(with-pre-hook! #'do-metadata-batch-set
  (fn [params body]
    (log-call "do-metadata-batch-set" params body)
    (validate-map params {:user string? :path string?})
    (validate-map body {:add sequential? :delete sequential?})
    (let [user (:user params)
          path (:path params)
          adds (:add body)
          dels (:delete body)]
      (log/warn (jargon-cfg))
      (when (pos? (count adds))
        (if-not (every? true? (check-avus adds))
          (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD :field "add"})))
      (when (pos? (count dels))
        (if-not (every? true? (check-avus dels))
          (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD :field "delete"}))))))

(with-post-hook! #'do-metadata-batch-set (log-func "do-metadata-batch-set"))

(defn do-metadata-delete
  "Entrypoint for the API that calls (metadata-delete). Parameter is a map
   with :user, :path, :attr, :value as keys. Values are strings."
  [{user :user path :path attr :attr value :value}]
  (metadata-delete user path attr value))

(with-pre-hook! #'do-metadata-delete
  (fn [params]
    (log-call "do-metadata-delete" params)
    (validate-map params {:user string?
                          :path string?
                          :attr string?
                          :value string?})))

(with-post-hook! #'do-metadata-delete (log-func "do-metadata-delete"))

(defn- get-metadata-template-avus
  "Gets a map containing AVUs for the given Metadata Template and the template's ID."
  [user-id data-id template-id]
  {:template_id template-id
   :avus (persistence/get-avus-for-metadata-template user-id data-id template-id)})

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
     (persistence/register-data-target data-id)
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
  (transaction
   (persistence/remove-avu-template-instances template-id [avu-id])
   (persistence/remove-avu avu-id))
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

(defn do-set-metadata-template-avus
  "Adds or Updates AVUs associated with a Metadata Template for the given user's data item."
  [{username :user} data-id template-id body]
  (set-metadata-template-avus username
                              (UUID/fromString data-id)
                              (UUID/fromString template-id)
                              body))

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

