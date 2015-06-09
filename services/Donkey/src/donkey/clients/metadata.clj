(ns donkey.clients.metadata
  (:require [cheshire.core :as cheshire]
            [donkey.clients.metadata.raw :as raw]
            [donkey.util.service :as service]))

(defn- parse-body
  [response]
  (service/decode-json (:body response)))

(defn list-templates
  []
  (parse-body (raw/list-templates)))

(defn get-template
  [template-id]
  (parse-body (raw/get-template template-id)))

(defn get-attribute
  [attr-id]
  (parse-body (raw/get-attribute attr-id)))

(defn admin-list-templates
  []
  (parse-body (raw/admin-list-templates)))

(defn admin-add-template
  [user-id template]
  (parse-body (raw/admin-add-template user-id (cheshire/encode template))))

(defn admin-update-template
  [user-id template-id template]
  (parse-body (raw/admin-update-template user-id template-id (cheshire/encode template))))

;; This function alias relies on get-template's error checking to throw an exception if a template
;; with the given ID doesn't exist.
(def validate-template-exists get-template)

(defn admin-delete-template
  [user-id template-id]
  (raw/admin-delete-template user-id template-id))
