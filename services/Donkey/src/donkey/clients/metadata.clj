(ns donkey.clients.metadata
  (:require [donkey.clients.metadata.raw :as raw]
            [donkey.util.service :as service]))

(defn list-templates
  []
  (->> (raw/list-templates)
       (:body)
       (service/decode-json)))
