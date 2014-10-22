(ns iplant-email.config
  (:require [clojure-commons.props :as props]
            [clojure-commons.config :as cc]
            [clojure.tools.logging :as log]))

(def config (ref nil))

(def listen-port
  (memoize
   (fn []
    (Integer/parseInt (get @config "iplant-email.app.listen-port")))))

(def smtp-host
  (memoize
   (fn []
    (get @config "iplant-email.smtp.host"))))

(def smtp-from-addr
  (memoize
   (fn []
     (get @config "iplant-email.smtp.from-address"))))

(defn load-config-from-file
  [cfg-path]
  (cc/load-config-from-file cfg-path config))
