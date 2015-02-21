(ns metadactyl.service.apps.de
  (:require [metadactyl.service.apps.de.listings :as listings]))

(deftype DeApps [user]
  metadactyl.protocols.Apps

  (listAppCategories [_ params]
    (listings/get-app-groups user params)))
