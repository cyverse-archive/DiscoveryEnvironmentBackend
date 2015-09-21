(ns data-info.services.metadata
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops :only [copy-stream]]
        [clj-jargon.metadata]
        [clj-jargon.validations :only [validate-path-lengths]]
        [kameleon.uuids :only [uuidify]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.clients.metadata :as metadata]
            [data-info.services.directory :as directory]
            [data-info.services.stat :as stat]
            [data-info.services.uuids :as uuids]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.paths :as paths]
            [data-info.util.validators :as validators]))


(defn- fix-unit
  "Used to replace the IPCRESERVED unit with an empty string."
  [avu]
  (if (= (:unit avu) paths/IPCRESERVED)
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
    paths/IPCRESERVED
    (:unit avu-map)))

(defn metadata-get
  "Returns the metadata for a path. Filters out system AVUs and replaces
   units set to ipc-reserved with an empty string."
  [user path]
  (with-jargon (cfg/jargon-cfg) [cm]
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
  (with-jargon (cfg/jargon-cfg) [cm]
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
  (with-jargon (cfg/jargon-cfg) [cm]
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
  (with-jargon (cfg/jargon-cfg) [cm]
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

(defn metadata-delete
  "Deletes an AVU from path on behalf of a user. attr and value should be strings."
  [user path attr value]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-writeable cm user path)
    (authorized-avus [{:attr attr :value value :unit ""}])
    (delete-metadata cm path attr value)
    {:path path :user user}))

(defn- stat-is-dir?
  [{:keys [type]}]
  (= :dir type))

(defn- segregate-files-from-folders
  "Takes a list of path-stat results and splits the folders and files into a map with :folders and
   :files keys, with the segregated lists as values."
  [folder-children]
  (zipmap [:folders :files]
    ((juxt filter remove) stat-is-dir? folder-children)))

(defn- format-template-avus
  "Takes a Metadata Template map and returns just its :avus list, adding the template ID to each AVU."
  [{:keys [template_id avus]}]
  (map #(assoc % :template_id template_id) avus))

(defn- get-metadata-template-avus
  [data-id]
  (mapcat format-template-avus (:templates (metadata/get-metadata-avus data-id))))

(defn- get-data-item-metadata-for-save
  "Adds a :metadata key to the given data-item, with a list of all IRODS and Template AVUs together
   as the key's value. If recursive? is true and data-item is a folder, then includes all files and
   subfolders (plus all their files and subfolders) with their metadata in the resulting stat map."
  [cm user recursive? {:keys [id path] :as data-item}]
  (let [metadata (list-path-metadata cm path)
        template-avus (get-metadata-template-avus (uuidify id))
        data-item (assoc data-item :metadata (concat metadata template-avus))
        path->metadata (comp (partial get-data-item-metadata-for-save cm user recursive?)
                             (partial stat/path-stat cm user))]
    (if (and recursive? (stat-is-dir? data-item))
      (merge data-item
             (segregate-files-from-folders
               (map path->metadata (directory/get-paths-in-folder user path))))
      data-item)))

(defn- build-metadata-for-save
  [cm user data-item recursive?]
  (-> (get-data-item-metadata-for-save cm user recursive? data-item)
      (dissoc :uuid)
      (json/encode {:pretty true})))

(defn metadata-save
  "Allows a user to export metadata from a file or folder with the given data-id to a file specified
   by dest."
  [user data-id dest recursive?]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [dest-dir (ft/dirname dest)
          src-data (uuids/path-for-uuid cm user data-id)
          src-path (:path src-data)]
      (validators/path-readable cm user src-path)
      (validators/path-exists cm dest-dir)
      (validators/path-writeable cm user dest-dir)
      (validators/path-not-exists cm dest)
      (validate-path-lengths dest)
      (when recursive?
        (validators/validate-num-paths-under-folder user src-path))

      (with-in-str (build-metadata-for-save cm user src-data recursive?)
        {:file (stat/decorate-stat cm user (copy-stream cm *in* user dest))}))))

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
    (dul/log-call "do-metadata-get" params)
    (validate-map params {:user string? :path string?})))

(with-post-hook! #'do-metadata-get (dul/log-func "do-metadata-get"))

(defn do-metadata-set
  "Entrypoint for the API. Calls (metadata-set). Parameter should be a map
   with :user and :path as keys. Values are strings."
  [{user :user path :path} body]
  (metadata-set user path body))

(with-pre-hook! #'do-metadata-set
  (fn [params body]
    (dul/log-call "do-metadata-set" params body)
    (validate-map params {:user string? :path string?})
    (validate-map body {:attr string? :value string? :unit string?})))

(with-post-hook! #'do-metadata-set (dul/log-func "do-metadata-set"))

(defn do-metadata-batch-set
  "Entrypoint for the API that calls (metadata-batch-set). Parameter is a map
   with :user and :path as keys. Values are strings."
  [{user :user path :path} body]
  (metadata-batch-set user path body))

(with-pre-hook! #'do-metadata-batch-set
  (fn [params body]
    (dul/log-call "do-metadata-batch-set" params body)
    (validate-map params {:user string? :path string?})
    (validate-map body {:add sequential? :delete sequential?})
    (let [user (:user params)
          path (:path params)
          adds (:add body)
          dels (:delete body)]
      (log/warn (cfg/jargon-cfg))
      (when (pos? (count adds))
        (if-not (every? true? (check-avus adds))
          (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD :field "add"})))
      (when (pos? (count dels))
        (if-not (every? true? (check-avus dels))
          (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD :field "delete"}))))))

(with-post-hook! #'do-metadata-batch-set (dul/log-func "do-metadata-batch-set"))

(defn do-metadata-delete
  "Entrypoint for the API that calls (metadata-delete). Parameter is a map
   with :user, :path, :attr, :value as keys. Values are strings."
  [{user :user path :path attr :attr value :value}]
  (metadata-delete user path attr value))

(with-pre-hook! #'do-metadata-delete
  (fn [params]
    (dul/log-call "do-metadata-delete" params)
    (validate-map params {:user string?
                          :path string?
                          :attr string?
                          :value string?})))

(with-post-hook! #'do-metadata-delete (dul/log-func "do-metadata-delete"))

(defn do-metadata-save
  "Entrypoint for the API. Calls (metadata-save)."
  [data-id {:keys [user]} {:keys [dest recursive]}]
  (metadata-save user (uuidify data-id) (ft/rm-last-slash dest) (boolean recursive)))
