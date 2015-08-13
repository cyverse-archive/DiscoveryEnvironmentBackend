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
  [template]
  (parse-body (raw/admin-add-template (cheshire/encode template))))

(defn admin-update-template
  [template-id template]
  (parse-body (raw/admin-update-template template-id (cheshire/encode template))))

(defn admin-delete-template
  [template-id]
  (raw/admin-delete-template template-id))
