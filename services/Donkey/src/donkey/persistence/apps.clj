(ns donkey.persistence.apps
  "Functions for storing and retrieving information about apps that can be executed
   within the DE, excluding external apps such as Agave apps."
  (:use [kameleon.entities :only [analysis_listing]]
        [korma.core]
        [korma.db :only [with-db]])
  (:require [donkey.util.db :as db]))

(defn load-app-details
  [app-ids]
  (with-db db/de
    (select analysis_listing
            (where {:id [in app-ids]}))))
