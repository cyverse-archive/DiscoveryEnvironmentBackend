(ns template-mover.core
  (:gen-class)
  (:require [clojure.pprint :refer [pprint]]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [honeysql.core :as sql]
            [honeysql.helpers :refer :all]
            [kameleon.pgpass :as pgpass]))

(def ^:private cli-options
  [["-h" "--host HOST" "Database host name"
    :default "localhost"
    :validate [(complement string/blank?) "Must not be blank."]]
   ["-p" "--port PORT" "Database port number"
    :default 5432
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 65536) "Must be an integer between 0 and 65536."]]
   ["-U" "--user USER" "Database username"
    :default "de"
    :validate [(complement string/blank?) "Must not be blank"]]
   ["-d" "--de-database DATABASE" "DE database name"
    :default "de"
    :validate [(complement string/blank?) "Must not be blank"]]
   ["-m" "--metadata-database DATABASE" "Metadata database name"
    :default "metadata"
    :validate [(complement string/blank?) "Must not be blank"]]
   ["-?" "--help" "Display this help message"]])

(defn- bad-usage
  [summary errors]
  (println "Invalid Usage:")
  (dorun (map (comp println (partial str "\t")) errors))
  (println)
  (println summary)
  (System/exit 1))

(defn- show-help
  [summary]
  (println summary)
  (System/exit 0))

(defn- prompt-for-password
  "Prompts the user for a password."
  [user]
  (print user "password: ")
  (flush)
  (.. System console readPassword))

(defn- get-password
  "Attempts to obtain the database password from the user's .pgpass file.  If
  the password can't be obtained from .pgpass, prompts the user for the
  password"
  [host port database user]
  (let [password (pgpass/get-password host port database user)]
    (if (nil? password)
      (if (nil? (System/console))
        (binding [*out* *err*]
          (println "No password supplied.")
          (System/exit 1))
        (prompt-for-password user))
      password)))

(defn- build-spec
  [host port db user password]
  {:subprotocol "postgresql"
   :subname     (str "//" host ":" port "/" db)
   :user        user
   :password    password})

(defn- get-value-types
  []
  (-> (select :id :name)
      (from :metadata_value_types)))

(defn- store-value-types
  [value-types]
  (-> (insert-into :value_types)
      (values value-types)))

(defn- get-attributes
  []
  (-> (select :name :description :required :value_type_id :id :created_by :modified_by
              :created_on :modified_on)
      (from :metadata_attributes)))

(defn- store-attributes
  [attributes]
  (-> (insert-into :attributes)
      (values attributes)))

(defn- get-attr-synonyms
  []
  (-> (select :attribute_id :synonym_id)
      (from :metadata_attr_synonyms)))

(defn- store-attr-synonyms
  [attr-synonyms]
  (-> (insert-into :attr_synonyms)
      (values attr-synonyms)))

(defn- get-attr-enum-values
  []
  (-> (select :id :attribute_id :value :is_default :display_order)
      (from :metadata_attr_enum_values)))

(defn- store-attr-enum-values
  [attr-enum-values]
  (-> (insert-into :attr_enum_values)
      (values attr-enum-values)))

(defn- get-templates
  []
  (-> (select :id :name :deleted :created_by :modified_by :created_on :modified_on)
      (from :metadata_templates)))

(defn- store-templates
  [templates]
  (-> (insert-into :templates)
      (values templates)))

(defn- get-template-attrs
  []
  (-> (select :template_id :attribute_id :display_order)
      (from :metadata_template_attrs)))

(defn- store-template-attrs
  [template-attrs]
  (-> (insert-into :template_attrs)
      (values template-attrs)))

(defn- migrate-table
  [source-db dest-db query-fn storage-statement-fn]
  (let [source-items (jdbc/query source-db (sql/format (query-fn)))]
    (when (seq source-items)
      (jdbc/execute! dest-db (sql/format (storage-statement-fn source-items))))))

(defn- template-instances-fk-constraint-removal-statement
  []
  "ALTER TABLE template_instances
       DROP CONSTRAINT IF EXISTS template_instances_template_id_fkey")

(defn- remove-template-instances-fk-constraint
  [metadata-db]
  (jdbc/execute! metadata-db [(template-instances-fk-constraint-removal-statement)]))

(defn- template-instances-fk-constraint-statement
  []
  "ALTER TABLE template_instances
      ADD CONSTRAINT template_instances_template_id_fkey
      FOREIGN KEY (template_id)
      REFERENCES templates(id)")

(defn- add-template-instances-fk-constraint
  [metadata-db]
  (jdbc/execute! metadata-db [(template-instances-fk-constraint-statement)]))

(defn- migrate-metadata-template-tables
  [de-db metadata-db]
  (try
    (->> [[get-value-types      store-value-types]
          [get-attributes       store-attributes]
          [get-attr-synonyms    store-attr-synonyms]
          [get-attr-enum-values store-attr-enum-values]
          [get-templates        store-templates]
          [get-template-attrs   store-template-attrs]]
         (map (partial apply migrate-table de-db metadata-db))
         (dorun))
    (remove-template-instances-fk-constraint metadata-db)
    (add-template-instances-fk-constraint metadata-db)
    (catch java.sql.SQLException e
      (log/error e)
      (when (.getNextException e)
        (log/error (.getNextException e)))
      (throw e))))

(defn- run-migration
  [{:keys [host port user de-database metadata-database]}]
  (let [password (get-password host port de-database user)
        de-spec  (build-spec host port de-database user password)
        md-spec  (build-spec host port metadata-database user password)]
    (with-open [de-conn (jdbc/get-connection de-spec)
                md-conn (jdbc/get-connection md-spec)]
      (migrate-metadata-template-tables
       (jdbc/add-connection de-spec de-conn)
       (jdbc/add-connection md-spec md-conn)))))

(defn -main
  [& args]
  (let [{:keys [options arguments summary errors]} (cli/parse-opts args cli-options)]
    (cond
     (seq errors)    (bad-usage summary errors)
     (:help options) (show-help summary)
     :else           (run-migration options))))
