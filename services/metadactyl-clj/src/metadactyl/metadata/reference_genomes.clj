(ns metadactyl.metadata.reference-genomes
  (:use [clojure.string :only [blank?]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.queries :only [get-user-id]]
        [korma.core]
        [korma.db]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.conversions :only [date->timestamp]]
        [metadactyl.util.service :only [success-response]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as error-codes]
            [clojure.tools.logging :as log]))

(defn- reference-genome-base-query
  "The base query used to list reference genomes."
  []
  (-> (select* genome_reference)
      (fields :id :name :path :deleted :created_on :last_modified_on
              [:created_by.username :created_by]
              [:last_modified_by.username :last_modified_by])
      (join created_by)
      (join last_modified_by)))

(defn get-reference-genomes
  "Lists all of the reference genomes in the database."
  [& uuids]
  (if (seq uuids)
    (select (reference-genome-base-query)
            (where {:id [in uuids]}))
    (select (reference-genome-base-query))))

(defn list-reference-genomes
  "Lists the reference genomes in the database."
  []
  (success-response {:genomes (get-reference-genomes)}))

(defn get-reference-genome
  [reference-genome-id]
  (success-response (assert-not-nil [:reference-genome-id reference-genome-id]
                      (first (get-reference-genomes [reference-genome-id])))))

(def ^:private valid-insert-fields
  [:id :name :path :deleted :created_by :created_on :last_modified_by :last_modified_on])

(def ^:prvate required-fields
  [:name :path :created_by :last_modified_by])

(def ^:private username-fields
  [:created_by :last_modified_by])

(defn- validate-field
  "Validates a single field in a reference genome."
  [genome field]
  (when (blank? (genome field))
    (throw+ {:error_code error-codes/ERR_BAD_OR_MISSING_FIELD
             field "empty required field"
             :genome genome})))

(defn- validate-username
  "Validates a username field in a reference genome."
  [genome field]
  (let [username (genome field)]
    (when-not (or (= "<public>" username) (re-find #"@" username))
      (throw+ {:error_code error-codes/ERR_BAD_OR_MISSING_FIELD
               field "username not fully qualified"
               :genome genome}))))

(defn- validate-reference-genome
  "Validates a reference genome for the reference genome replacement service."
  [genome]
  (dorun (map #(validate-field genome %) required-fields))
  (dorun (map #(validate-username genome %) username-fields))
  genome)

(defn- format-valid-genome-fields
  "Formats the valid reference genome fields for insertion into the database."
  [{:keys [created_by last_modified_by created_on last_modified_on] :as genome}]
  (-> genome
      (select-keys valid-insert-fields)
      (assoc :created_by       (get-user-id created_by)
             :last_modified_by (get-user-id last_modified_by)
             :created_on       (or (date->timestamp created_on) (sqlfn now))
             :last_modified_on (or (date->timestamp last_modified_on) (sqlfn now)))))

(defn- parse-reference-genome
  "Parses a reference genome for the reference genome replacement service.  The
   result of this function can be passed directly to an insert for the genome
   reference table."
  [genome]
  (-> genome
      validate-reference-genome
      format-valid-genome-fields))

(defn put-reference-genomes
  "Replaces the existing reference genomes in the database."
  [genomes]
  (transaction
   (exec-raw "TRUNCATE genome_reference")
   (insert genome_reference
           (values (map parse-reference-genome genomes)))))

(defn replace-reference-genomes
  "Replaces the reference genomes in the database with a new set of reference genomes."
  [body]
  (log/warn (:username current-user) "replacing reference genomes")
  (put-reference-genomes (:genomes body))
  (list-reference-genomes))
