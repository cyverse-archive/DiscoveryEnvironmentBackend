(ns metadactyl.service.apps.agave
  (:require [metadactyl.service.apps.agave.listings :as listings]))

(deftype AgaveApps [agave user-has-access-token?]
  metadactyl.protocols.Apps

  (listAppCategories [_ {:keys [hpc]}]
    (when-not (and hpc (.equalsIgnoreCase hpc "false"))
      [(.hpcAppGroup agave)]))

  (listAppsInCategory [_ category-id params]
    (when (= category-id (:id (.hpcAppGroup agave)))
      (listings/list-apps agave category-id params))))
