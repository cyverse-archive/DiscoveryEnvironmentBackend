(ns conrad.kormadb
  (:use [korma.db]
        [conrad.config]
        [conrad.database])
  (:require [clojure.tools.logging :as log]))

(defn define-database
  "Defines the database connection to use from within Clojure."
  []
  (let [spec (db-spec)]
    (defonce de (create-db spec))
    (default-connection de)))
