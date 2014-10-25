(ns metadactyl.tools
  (:use [metadactyl.persistence.app-metadata :only [add-tool]]
        [korma.db :only [transaction]])
  (:require [clojure-commons.error-codes :as cc-errs]
   [metadactyl.util.service :as service]))

(defn add-tools
  "Adds a list of tools to the database, returning a list of IDs of the tools added."
  [{:keys [tools]}]
  (transaction
    (let [tool-ids (map add-tool tools)]
      (service/success-response {:tool_ids tool-ids}))))
