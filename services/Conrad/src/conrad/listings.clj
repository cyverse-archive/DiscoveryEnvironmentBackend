(ns conrad.listings
  (:use [conrad.app-listings]
        [conrad.category-listings]
        [conrad.database])
  (:require [cheshire.core :as cheshire]
            [clojure.java.jdbc :as jdbc]))

(defn get-public-categories []
  (jdbc/with-connection (db-connection)
    (cheshire/encode (list-public-categories-without-apps))))

(defn get-category-with-apps [category-id]
  (jdbc/with-connection (db-connection)
    (cheshire/encode (list-category-with-apps category-id))))

(defn get-components-in-app [app-id]
  (jdbc/with-connection (db-connection)
    (cheshire/encode (list-deployed-components-in-app app-id))))
