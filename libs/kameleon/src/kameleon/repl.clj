(ns kameleon.repl
  (:use [korma.db]))

(defn db-spec
  "Creates the database connection spec to use when accessing the database
   using Korma."
  [dbname hostname port user password]
  {:subprotocol "postgresql"
   :classname   "org.postgresql.Driver"
   :subname     (str "//" hostname ":" port "/" dbname)
   :user        user
   :password    password})
