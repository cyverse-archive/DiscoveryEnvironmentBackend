(ns infosquito.core
  "This namespace defines the entry point for Infosquito. All state should be in here."
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :as ss]
            [clojure-commons.config :as config]
            [infosquito.actions :as actions]
            [infosquito.exceptions :as exn]
            [infosquito.icat :as icat]
            [infosquito.messages :as messages]
            [infosquito.props :as props])
  (:import [java.util Properties]))


(defn- log-invalid-prop
  [prop-name]
  (log/error "configuration setting," prop-name ", is undefined"))


(defn- validate-props
  [p]
  (when-not (props/validate p log-invalid-prop)
    (ss/throw+ "The configuration parameters are invalid.")))


(defn- load-config-from-file
  [config-path]
  (let [p (ref nil)]
    (config/load-config-from-file nil config-path p)
    (validate-props @p)
    @p))


(defn- load-config-from-zookeeper
  []
  (let [p (ref nil)]
    (config/load-config-from-zookeeper p "infosquito")
    (validate-props @p)
    @p))


(defn- exit
  [msg]
  (log/fatal msg)
  (System/exit 1))


(defn- update-props
  [load-props old-props]
  (ss/try+
   (let [new-props (load-props)]
     (validate-props new-props)
     (when-not (= old-props new-props)
       (config/log-config new-props))
     new-props)
   (catch IllegalStateException t (exit (or (.getMessage t) (str t))))
   (catch Object _ (exit "Unable to load the configuration parameters."))))


(defmacro ^:private trap-exceptions!
  [& body]
  `(ss/try+
     (do ~@body)
     (catch Object o# (log/error (exn/fmt-throw-context ~'&throw-context)))))


(defn- parse-args
  [args]
  (cli/cli
   args
   ["-c" "--config" "sets the local configuration file to be read, bypassing Zookeeper"]
   ["-r" "--reindex" "reindex the iPlant Data Store and exit" :flag true]
   ["-?" "-h" "--help" "show help and exit" :flag true]))


(defn- get-props
  [opts]
  (let [props-ref (ref (if (:config opts)
                         (load-config-from-file (:config opts))
                         (load-config-from-zookeeper)))]
    (config/log-config props-ref)
    @props-ref))


(defn -main
  [& args]
  (let [[opts _ help-text] (parse-args args)]
    (cond (:help opts)    (println help-text)
          (:reindex opts) (actions/reindex (get-props opts))
          :else           (messages/repeatedly-connect (get-props opts)))))
