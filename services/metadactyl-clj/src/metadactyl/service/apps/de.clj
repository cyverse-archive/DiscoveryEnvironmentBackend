(ns metadactyl.service.apps.de
  (:require [metadactyl.service.apps.de.listings :as listings]))

(deftype DeApps [user]
  metadactyl.protocols.Apps

  (listAppCategories [_ params]
    (listings/get-app-groups user params))

  (hasCategory [_ category-id]
    (listings/has-category category-id))

  (listAppsInCategory [_ category-id params]
    (listings/list-apps-in-group user category-id params)))
