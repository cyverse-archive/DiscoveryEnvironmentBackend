(ns info-typer.irods
  (:use [clj-jargon.item-ops :only [input-stream]])
  (:require [heuristomancer.core :as hm]
            [clojure.tools.logging :as log]
            [info-typer.config :as cfg]))


(defn- get-file-type
  "Uses heuristomancer to determine a the file type of a file."
  [cm path]
  (let [result (hm/identify (input-stream cm path) (cfg/filetype-read-amount))]
    (if-not (nil? result)
      (name result)
      result)))


(defn content-type
  "Determines the filetype of path. Reads in a chunk, writes it to a temp file, runs it
   against the configured script. If the script can't identify it, it's passed to Tika."
  [cm path]
  (log/info "in content-type")

  (let [script-type (get-file-type cm path)]
    (log/info "Path " path " has a type of " script-type " from the script.")
    (if (or (nil? script-type) (empty? script-type))
      ""
      script-type)))
