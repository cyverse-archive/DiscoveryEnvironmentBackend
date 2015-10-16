(ns iplant_groups.service.attributes
  (:require [iplant_groups.clients.grouper :as grouper]
            [iplant_groups.service.format :as fmt]))

(defn add-attribute-name
  [{:keys [name description display_extension attribute_definition]} {:keys [user]}]
  (let [attribute-name (grouper/add-attribute-name user (:id attribute_definition) name display_extension description)]
    (fmt/format-attribute-name attribute-name)))
