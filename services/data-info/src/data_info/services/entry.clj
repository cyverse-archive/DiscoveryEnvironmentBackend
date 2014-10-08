(ns data-info.services.entry
  "This namespace provides the business logic for all entries endpoints."
  (:require [data-info.services.exists :as exists]
            [data-info.services.updown :as updown]))


(defn id-exists?
  [params]
  (exists/exists? params))


(defn get-by-path
  [path-in-zone params]
  (updown/dispatch-entries-path path-in-zone params))
