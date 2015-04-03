(ns metadactyl.util.json
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log]))

(defn log-json
  [description m]
  (->> (json/encode m {:pretty true})
       (str description ": ")
       (log/info))
  m)
