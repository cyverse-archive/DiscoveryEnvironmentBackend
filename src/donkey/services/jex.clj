(ns donkey.services.jex
  (:require [donkey.util.service :as svc]
            [donkey.util.config :as cfg]))

(defn stop-analysis
  [request uuid]
  (let [stop-url (svc/build-url (cfg/jex-base-url) "stop" uuid)]
    (svc/forward-delete stop-url request)))

