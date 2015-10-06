(ns facepalm.c187-2014042001
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.6:20140420.01")

(defn- add-user-pk-uniqueness
  []
  (println "\t* adding uniqueness constraint on the id column")
  (exec-raw
   "ALTER TABLE users ADD UNIQUE (id)"))

(defn convert
  []
  (println "\t* making the users.id unique")
  (add-user-pk-uniqueness))
