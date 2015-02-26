(ns metadactyl.service.apps.combined
  (:use [metadactyl.service.apps.combined.util :as util]
        [metadactyl.util.assertions :only [assert-not-nil]]))

(deftype CombinedApps [clients]
  metadactyl.protocols.Apps

  (listAppCategories [_ params]
    (apply concat (map #(.listAppCategories % params) clients)))

  (hasCategory [_ category-id]
    (some #(.hasCategory % category-id) clients))

  (listAppsInCategory [_ category-id params]
    (assert-not-nil
     [:category-id category-id]
     (when-let [client (first (filter #(.hasCategory % category-id) clients))]
       (.listAppsInCategory client category-id params))))

  (searchApps [_ search-term params]
    (->> (map #(.searchApps % search-term {}) clients)
         (remove nil?)
         (combine-app-search-results params))))
