(ns metadactyl.service.apps.combined)

(deftype CombinedApps [clients]
  metadactyl.protocols.Apps

  (listAppCategories [_ params]
    (apply concat (map #(.listAppCategories % params) clients))))
