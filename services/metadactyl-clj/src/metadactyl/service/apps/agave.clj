(ns metadactyl.service.apps.agave)

(deftype AgaveApps [agave user-has-access-token?]
  metadactyl.protocols.Apps

  (listAppCategories [_ {:keys [hpc]}]
    (when-not (and hpc (.equalsIgnoreCase hpc "false"))
      [(.hpcAppGroup agave)])))
