(ns facepalm.c186-2014040401
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.6:20140404.01")

(def ^:private email-templates
  [["1fb4295b-684e-4657-afab-6cc0912312b1", "tool_request_submitted"]
   ["afbbcda8-49c3-47c0-9f28-de87cbfbcbd6", "tool_request_pending"]
   ["b15fd4b9-a8d3-48ec-bd29-b0aacb51d335", "tool_request_evaluation"]
   ["031d4f2c-3880-4483-88f8-e6c27c374340", "tool_request_installation"]
   ["e4a0210c-663c-4943-bae9-7d2fa7063301", "tool_request_validation"]
   ["5ed94200-7565-45d8-b576-d7ff839e9993", "tool_request_completion"]
   ["461f24ee-5521-461a-8c20-c400d912fb2d", "tool_request_failed"]])

(defn- update-email-template
  "Updates the email template column for a tool request status code."
  [[uuid-str template-name]]
  (update :tool_request_status_codes
          (set-fields {:email_template template-name})
          (where {:id (UUID/fromString uuid-str)})))

(defn- add-tool-request-email-template-column
  "Adds the email_template column to the tool_request_status_codes table."
  []
  (println "\t* adding the email_template column to the tool_request_status_codes table")
  (exec-raw
   "ALTER TABLE tool_request_status_codes
    ADD COLUMN email_template VARCHAR(64)")
  (dorun (map update-email-template email-templates)))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (add-tool-request-email-template-column))
