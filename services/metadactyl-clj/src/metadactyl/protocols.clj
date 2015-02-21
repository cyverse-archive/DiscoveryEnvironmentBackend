(ns metadactyl.protocols)

(defprotocol Apps
  "A protocol used to provide an abstraction layer for dealing with app metadata."
  (listAppCategories [_ params]))
