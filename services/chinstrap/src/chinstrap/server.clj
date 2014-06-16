(ns chinstrap.server
  (:gen-class)
  (:use [chinstrap.config :only [listen-port]]
        [chinstrap.db])
  (:require [noir.server :as server]
            [clojure.tools.logging :as log]))

(server/load-views-ns 'chinstrap.views)

(defn -main [& m]
  (try
    (db-config)
    (catch Exception e
      (log/error e "Configuration Failed")))
  
  (let [mode (keyword (or (first m) :dev))
        port (listen-port)]
    (server/start port {:mode mode
                        :ns 'chinstrap})))

