(ns data-info.services.filesystem.status
  (:require [clojure.tools.logging :as log]
            [clj-jargon.init :as init]
            [clj-jargon.item-info :as item]
            [clojure-commons.error-codes :as ce]
            [data-info.services.filesystem.icat :as icat]))


(defn ^Boolean irods-running?
  "Determines whether or not iRODS is running."
  []
  (try
    (init/with-jargon (icat/jargon-cfg) [cm]
      (item/exists? cm (:home cm)))
    (catch Exception e
      (log/error "Error performing iRODS status check:")
      (log/error (ce/format-exception e))
      false)))
