(ns iplant-email.config
  (:require [clojure-commons.props :as props]
            [clojure-commons.clavin-client :as cl]
            [clojure.tools.logging :as log]))

(def config (atom nil))

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

(defn load-config-from-zookeeper
  []
  (let [zkprops (props/parse-properties "zkhosts.properties")
        zkurl   (get zkprops "zookeeper")]
    (cl/with-zk zkurl
      (when (not (cl/can-run?))
        (log/warn "THIS APPLICATION CANNOT RUN ON THIS MACHINE. SO SAYETH ZOOKEEPER.")
        (log/warn "THIS APPLICATION WILL NOT EXECUTE CORRECTLY."))

      (reset! config (cl/properties "iplant-email")))))
