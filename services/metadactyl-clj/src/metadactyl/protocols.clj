(ns metadactyl.protocols)

(defprotocol Apps
  "A protocol used to provide an abstraction layer for dealing with app metadata."
  (listAppCategories [_ params])
  (hasCategory [_ category-id])
  (listAppsInCategory [_ category-id params]))
