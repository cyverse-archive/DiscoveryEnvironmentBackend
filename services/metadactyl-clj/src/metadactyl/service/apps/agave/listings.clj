(ns metadactyl.service.apps.agave.listings
  (:use [metadactyl.service.util :only [sort-apps apply-offset apply-limit]]
        [slingshot.slingshot :only [try+]])
  (:require [clojure-commons.error-codes :as ce]))

(defn list-apps
  [agave category-id params]
  (-> (.listApps agave)
      (sort-apps params)
      (apply-offset params)
      (apply-limit params)))

(defn search-apps
  [agave search-term params]
  (try+
   (-> (.searchApps agave search-term)
       (sort-apps params)
       (apply-offset params)
       (apply-limit params))
   (catch [:error_code ce/ERR_UNAVAILABLE] _ nil)))
