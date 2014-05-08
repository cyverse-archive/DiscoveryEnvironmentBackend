(ns common-cfg.cfg
  (:use [medley.core])
  (:require [clojure.edn :as edn]
            [bouncer [core :as b] [validators :as v]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [filevents.core :refer [watch]]
            [clojure.java.io :refer [reader]]
            [me.raynes.fs :as fs])
  (:import [java.util Properties]))

(timbre/refer-timbre)

(def cfg
  "A ref for storing the combined configuration properties."
  (ref nil))

(def cmd-line
  "A ref for storing the props passed in on the command-line."
  (ref nil))

(def filters
  "A ref containing a set of keys to filter from configs before
   printing them."
  (ref #{}))

(def defaults
  "A ref containing the default values for the config."
  (ref {}))

(def validators
  "A ref containing a map of bouncer validators for the config
   file."
  (ref {}))

(defn configure-logging
  []
  (when (:log-level @cfg)
    (timbre/set-level! (:log-level @cfg)))
  (when (:log-file @cfg)
    (timbre/set-config! [:appenders :rotor]
                        {:enabled? true
                         :async? false
                         :max-messages-per-msecs nil
                         :fn rotor/appender-fn})
    (timbre/set-config! [:shared-appender-config :rotor]
                        {:path     (:log-file @cfg)
                         :max-size (:log-size @cfg)
                         :backlog  (:log-backlog @cfg)})))

(defn pprint-to-string
  [m]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (clojure.pprint/pprint m))
    (str sw)))

(defn- valid-config?
  [cfg]
  (let [errs (first (b/validate cfg @validators))]
    (when errs
      (error (pprint-to-string errs)))
    (not errs)))

(v/defvalidator stringv
  {:default-message-format "%s must be a string", :optional true}
  [s]
  (string? s))

(v/defvalidator keywordv
  {:default-message-format "%s must be a keyword", :optional true}
  [k]
  (keyword? k))

(defn- loggable-config
  [cfg bad-keys]
  (pprint-to-string
   (filter-keys #(not (contains? bad-keys %)) cfg)))

(defn load-properties
  "Loads a properties file into a map, turning the keys into keywords."
  [cfg-path]
  (let [p (Properties.)]
    (with-open [r (reader (fs/normalized-path (fs/expand-home cfg-path)))]
      (.load p r)
      (into {} (map #(vector (keyword (first %1)) (second %1)) 
                    (into {} (seq p)))))))

(defn load-edn
  [cfg-path]
  (edn/read-string (slurp (fs/normalized-path (fs/expand-home cfg-path)))))

(defn load-cfg-file
  [cfg-path]
  (cond
   (.endsWith cfg-path ".properties")
   (load-properties cfg-path)

   (.endsWith cfg-path ".edn")
   (load-edn cfg-path)

   :else
   (throw (Exception. "Unrecognized configuration file type"))))

(defn- load-config-from-file
  "Loads the configuration settings from a file."
  []
  (let [cfgfile (load-cfg-file (:config @cmd-line))]
    (when-not (valid-config? cfgfile)
      (error "Config file has errors, exiting.")
      (System/exit 1))
    (dosync (ref-set cfg (merge @defaults cfgfile @cmd-line)))
    (configure-logging)
    (info "Config file settings:\n" (loggable-config cfgfile @filters))
    (info "Command-line settings:\n" (loggable-config @cmd-line @filters))
    (info "Combined settings:\n" (loggable-config @cfg @filters))))

(defn load-config
  [options]
  (dosync (ref-set cmd-line options))
  (load-config-from-file))
