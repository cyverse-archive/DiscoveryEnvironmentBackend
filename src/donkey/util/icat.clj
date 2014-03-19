(ns donkey.util.icat
  (:use [clj-icat-direct.icat :only [icat-db-spec setup-icat]])
  (:require [donkey.util.config :as cfg]
            [clojure.tools.logging :as log]))

(defn- spec
  []
  (icat-db-spec
    (cfg/icat-host)
    (cfg/icat-user)
    (cfg/icat-password)
    :port (cfg/icat-port)
    :db   (cfg/icat-db)))

(defn configure-icat
  "Configures the connection pool to the ICAT database."
  []
  (log/warn "[ICAT] set up ICAT connection.")
  (setup-icat (spec)))