(ns metadata.services.templates
  (:use [clojure-commons.core :only [remove-nil-values]])
  (:require [metadata.persistence.templates :as tp]))

(defn list-templates
  []
  {:metadata_templates (mapv remove-nil-values (tp/list-templates))})
