(ns metadactyl.persistence.tool-requests
  "Functions for storing and retrieving information about tool requests."
  (:use [korma.core :exclude [update]]
        [korma.db :only [with-db]]))

(def ^:private default-email-template "tool_request_updated")

(defn email-template-for
  "Determines the name of the email template to use for a tool request status code."
  [status]
  (or ((comp :email_template first)
       (select :tool_request_status_codes
               (fields :email_template)
               (where {:name status})))
      default-email-template))
