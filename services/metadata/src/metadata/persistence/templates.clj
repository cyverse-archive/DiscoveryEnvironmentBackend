(ns metadata.persistence.templates
  (:use [korma.core]))

(defn- add-deleted-where-clause
  [query hide-deleted?]
  (if hide-deleted?
    (where query {:deleted false})
    query))

(defn list-templates
  ([]
     (list-templates true))
  ([hide-deleted?]
     (select :templates
             (fields :id :name :deleted :created_by :created_on :modified_by :modified_on)
             (add-deleted-where-clause hide-deleted?)
             (order :name))))
