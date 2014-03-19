(ns facepalm.c186-2014022701
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.6:20140227.01")

(defn- update-tool-request-status-code-name-length
  "Increases the length of the name field in the tool_request_status_codes table."
  []
  (println "\t* increasing the tool request status code name length")
  (exec-raw "ALTER TABLE tool_request_status_codes ALTER name TYPE VARCHAR(64)"))

(def ^:private
  new-tool-request-status-code-ids
  [["1fb4295b-684e-4657-afab-6cc0912312b1" "Submitted"]
   ["afbbcda8-49c3-47c0-9f28-de87cbfbcbd6" "Pending"]
   ["b15fd4b9-a8d3-48ec-bd29-b0aacb51d335" "Evaluation"]
   ["031d4f2c-3880-4483-88f8-e6c27c374340" "Installation"]
   ["e4a0210c-663c-4943-bae9-7d2fa7063301" "Validation"]
   ["5ed94200-7565-45d8-b576-d7ff839e9993" "Completion"]
   ["461f24ee-5521-461a-8c20-c400d912fb2d" "Failed"]])

(defn- drop-tool-request-status-code-constraints
  "Drops the constraints for the tool_request_status_codes table."
  []
  (exec-raw
   "ALTER TABLE tool_request_statuses
    DROP CONSTRAINT tool_request_statuses_tool_request_status_code_id_fkey")
  (exec-raw
   "ALTER TABLE tool_request_status_codes
    DROP CONSTRAINT tool_request_status_codes_pkey"))

(defn- insert-new-status-code-id
  "Inserts a new ID into a single row of the tool_request_status_codes_table."
  [[uuid status-code]]
  (update :tool_request_status_codes
          (set-fields {:id (UUID/fromString uuid)})
          (where {:name status-code})))

(defn- insert-new-status-code-ids
  "Inserts the new IDs into the tool_request_status_codes_table."
  []
  (exec-raw
   "ALTER TABLE tool_request_status_codes
    RENAME COLUMN id TO old_id")
  (exec-raw
   "ALTER TABLE tool_request_status_codes
    ADD COLUMN id UUID")
  (dorun (map insert-new-status-code-id new-tool-request-status-code-ids))
  (exec-raw
   "ALTER TABLE tool_request_status_codes
    ALTER COLUMN id SET NOT NULL"))

(defn- update-status-code-fks
  "Updates the status code foreign keys."
  []
  (exec-raw
   "ALTER TABLE tool_request_statuses
    RENAME COLUMN tool_request_status_code_id TO old_status_code_id")
  (exec-raw
   "ALTER TABLE tool_request_statuses
    ADD COLUMN tool_request_status_code_id UUID")
  (exec-raw
   "UPDATE tool_request_statuses
    SET tool_request_status_code_id = sc.id
    FROM tool_request_status_codes sc
    WHERE old_status_code_id = sc.old_id")
  (exec-raw
   "ALTER TABLE tool_request_statuses
    DROP COLUMN old_status_code_id")
  (exec-raw
   "ALTER TABLE tool_request_statuses
    ALTER COLUMN tool_request_status_code_id SET NOT NULL"))

(defn- drop-old-status-code-ids
  "Drops the old status code ID column."
  []
  (exec-raw
   "ALTER TABLE tool_request_status_codes
    DROP COLUMN old_id"))

(defn- add-tool-request-status-code-constraints
  "Re-inserts the tool request status code constraints."
  []
  (exec-raw
   "ALTER TABLE tool_request_status_codes
    ADD PRIMARY KEY(id)")
  (exec-raw
   "ALTER TABLE tool_request_statuses
    ADD CONSTRAINT tool_request_statuses_tool_request_status_code_id_fkey
    FOREIGN KEY(tool_request_status_code_id)
    REFERENCES tool_request_status_codes(id)"))

(defn- change-tool-request-status-code-id-to-uuid
  "Converts the tool request status code identifiers to UUIDs."
  []
  (println "\t* changing tool request status code IDs to UUIDs")
  (drop-tool-request-status-code-constraints)
  (insert-new-status-code-ids)
  (update-status-code-fks)
  (drop-old-status-code-ids)
  (add-tool-request-status-code-constraints))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (update-tool-request-status-code-name-length)
  (change-tool-request-status-code-id-to-uuid))
