(ns donkey.services.filesystem.metadata
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.metadata-template-avus :only [copy-metadata-template-avus]]
        [donkey.services.filesystem.validators]
        [kameleon.uuids :only [uuidify]]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.metadata]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.data.codec.base64 :as b64]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.services.filesystem.icat :as icat]
            [donkey.services.filesystem.uuids :as uuids]
            [donkey.services.filesystem.validators :as validators]
            [donkey.util.config :as cfg]))

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
  (re-find ipc-regex (:attr avu)))

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
  (remove
   ipc-avu?
   (map fix-unit (get-metadata cm (ft/rm-last-slash path)))))

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
  (with-jargon (icat/jargon-cfg) [cm]
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
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (when (= "failure" (:status avu-map))
      (throw+ {:error_code ERR_INVALID_JSON}))
    (validators/path-exists cm path)
    (validators/path-writeable cm user path)
    (authorized-avus [avu-map])
    {:path (common-metadata-set cm path avu-map)
     :user user}))

(defn admin-metadata-set
  "Adds the AVU to path, bypassing user permission checks. See (metadata-set)
   for the AVU map format."
  [path avu-map]
  (with-jargon (icat/jargon-cfg) [cm]
    (when (= "failure" (:status avu-map))
      (throw+ {:error_code ERR_INVALID_JSON}))
    (validators/path-exists cm path)
    (validators/path-writeable cm (cfg/irods-user) path)
    (common-metadata-set cm path avu-map)))

(defn- metadata-batch-set
  "Adds and deletes metadata on path for a user. add-dels should be in the
   following format:
   {
      :delete [{:attr :value :unit}]
      :add [{:attr :value :unit}]
   }
   All value in the maps should be strings, just like with (metadata-set)."
  [user path adds-dels]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-writeable cm user path)
    (authorized-avus (:delete adds-dels))
    (authorized-avus (:add adds-dels))
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

(defn- find-attributes
  [cm attrs path]
  (let [matching-avus (get-attributes cm attrs path)]
    (if-not (empty? matching-avus)
      {:path path
       :avus matching-avus}
      nil)))

(defn- validate-batch-add-attrs
  "Throws an error if any of the given paths already have metadata set with any of the given attrs."
  [cm paths attrs]
  (let [duplicates (remove nil? (map (partial find-attributes cm attrs) paths))]
    (when-not (empty? duplicates)
      (validators/duplicate-attrs-error duplicates))))

(defn- metadata-batch-add-to-path
  "Adds metadata to the given path. If the destination path already has an AVU with the same attr
   and value as one from the given avus list, that AVU is not added."
  [cm path avus]
  (loop [avus-current (get-metadata cm path)
         avus-to-add  avus]
    (when-not (empty? avus-to-add)
      (let [{:keys [attr value] :as avu} (first avus-to-add)
            new-unit (reserved-unit avu)]
        (if-not (attr-value? avus-current attr value)
          (add-metadata cm path attr value new-unit))
        (recur (conj avus-current {:attr attr :value value :unit new-unit}) (rest avus-to-add))))))

(defn- metadata-batch-add
  "Adds metadata to the given paths for a user. The avu map should be in the
   following format:
   {
      :paths []
      :avus [{:attr :value :unit}]
   }
   All paths and values in the maps should be strings, just like with (metadata-set)."
  [user force? {:keys [paths avus]}]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/all-paths-exist cm paths)
    (validators/all-paths-writeable cm user paths)
    (authorized-avus avus)
    (let [paths (set (map ft/rm-last-slash paths))
          attrs (set (map :attr avus))]
      (if-not force?
        (validate-batch-add-attrs cm paths attrs))
      (doseq [path paths]
        (metadata-batch-add-to-path cm path avus))
      {:paths paths :user user})))

(defn- metadata-copy
  "Copies all IRODS AVUs visible to the client, and Metadata Template AVUs, from the data item with
   src-id to the items with dest-ids. When the 'force?' parameter is set, additional validation is
   performed."
  [user force? src-id dest-ids]
  (with-jargon (icat/jargon-cfg) [cm]
    (transaction
      (let [{src-path :src dest-paths :paths :as results} (copy-metadata-template-avus
                                                            cm user force? src-id dest-ids)
            irods-avus (list-path-metadata cm src-path)]
        (doseq [path dest-paths]
          (metadata-batch-add-to-path cm path irods-avus))
        results))))

(defn metadata-delete
  "Deletes an AVU from path on behalf of a user. attr and value should be strings."
  [user path attr value]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-writeable cm user path)
    (authorized-avus [{:attr attr :value value :unit ""}])
    (delete-metadata cm path attr value)
    {:path path :user user}))

(defn- check-avus
  [avus]
  (mapv
   #(and (map? %1)
         (contains? %1 :attr)
         (contains? %1 :value)
         (contains? %1 :unit))
    avus))

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
      (log/warn (icat/jargon-cfg))
      (when (pos? (count adds))
        (if-not (every? true? (check-avus adds))
          (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD :field "add"})))
      (when (pos? (count dels))
        (if-not (every? true? (check-avus dels))
          (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD :field "delete"}))))))

(with-post-hook! #'do-metadata-batch-set (log-func "do-metadata-batch-set"))

(defn do-metadata-batch-add
  "Entrypoint for the API that calls (metadata-batch-add). Body is a map with :avus and :paths keys."
  [{:keys [user force]} body]
  (metadata-batch-add user (Boolean/parseBoolean force) body))

(with-pre-hook! #'do-metadata-batch-add
  (fn [params {:keys [paths avus] :as body}]
    (log-call "do-metadata-batch-add" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential? :avus sequential?})
    (validate-field :paths paths (comp pos? count))
    (validate-num-paths paths)
    (validate-field :avus avus (comp pos? count))
    (validate-field :avus avus (comp (partial every? true?) check-avus))
    (log/info (icat/jargon-cfg))))

(with-post-hook! #'do-metadata-batch-add (log-func "do-metadata-batch-add"))

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

(defn do-metadata-copy
  "Entrypoint for the API that calls (metadata-copy)."
  [{:keys [user]} data-id force {dest-ids :destination_ids}]
  (metadata-copy user (Boolean/parseBoolean force) (uuidify data-id) (map uuidify dest-ids)))

(with-pre-hook! #'do-metadata-copy
  (fn [{:keys [user] :as params} data-id force {dest-ids :destination_ids :as body}]
    (log-call "do-metadata-copy" params data-id body)
    (validate-map params {:user string?})
    (validate-map body {:destination_ids sequential?})
    (validate-num-paths dest-ids)))

(with-post-hook! #'do-metadata-copy (log-func "do-metadata-copy"))

(defn do-metadata-save
  "Forwards request to data-info service."
  [data-id params body]
  (let [url (url/url (cfg/data-info-base-url) "data" data-id "metadata" "save")
        req-map {:query-params (select-keys params [:user])
                 :content-type :json
                 :body         (json/encode body)}]
    (http/post (str url) req-map)))

(with-pre-hook! #'do-metadata-save
  (fn [data-id params body]
    (log-call "do-metadata-save" params body)
    (validate-map params {:user string?})))

(with-post-hook! #'do-metadata-save (log-func "do-metadata-save"))
