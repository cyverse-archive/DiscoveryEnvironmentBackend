(ns metadactyl.service.apps.combined)

(deftype CombinedApps [clients]
  metadactyl.protocols.Apps

  (listAppCategories [_ params]
    (map #(.listAppCategories % params) clients)))
