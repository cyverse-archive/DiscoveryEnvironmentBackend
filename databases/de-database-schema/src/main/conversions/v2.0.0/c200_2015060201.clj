(ns facepalm.c200-2015060201
    (:use [korma.core]))

(def ^:private version
    "The destination database version."
    "2.0.0:20150602.01")

(defn- fix-de-wc-settings
  []
  (println "\t* Adding network mode and entrypoint to the DE WC container")
  (exec-raw
    "UPDATE ONLY container_settings
        SET network_mode = 'none',
            entrypoint = 'wc'
      WHERE tools_id = '85cf7a33-386b-46fe-87c7-8c9d59972624';"))

(defn convert
  []
  (println "Performing the conversion for" version)
  (fix-de-wc-settings))
