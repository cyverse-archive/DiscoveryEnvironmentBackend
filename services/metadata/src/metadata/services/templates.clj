(ns metadata.services.templates
  (:use [clojure-commons.core :only [remove-nil-values]]
        [korma.db :only [transaction]])
  (:require [clojure-commons.assertions :as ca]
            [metadata.persistence.templates :as tp]))

(defn list-templates
  []
  {:metadata_templates (mapv remove-nil-values (tp/list-templates))})

(defn view-template
  [template-id]
  (-> (tp/view-template template-id)
      (ca/assert-found "metadata template" template-id)
      (remove-nil-values)))

(defn view-attribute
  [attr-id]
  (-> (tp/view-attribute attr-id)
      (ca/assert-found "metadata attribute" attr-id)
      (remove-nil-values)))

(defn admin-list-templates
  []
  {:metadata_templates (mapv remove-nil-values (tp/list-templates false))})

(defn add-template
  [{:keys [user-id]} template]
  (transaction (view-template (tp/add-template user-id template))))

(defn update-template
  [{:keys [user-id]} template-id template]
  (transaction (view-template (tp/update-template user-id template-id template))))
