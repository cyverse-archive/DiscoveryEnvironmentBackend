(ns metadata.services.templates
  (:use [clojure-commons.core :only [remove-nil-values]])
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
