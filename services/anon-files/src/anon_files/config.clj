(ns anon-files.config
  (:use [slingshot.slingshot :only [try+ throw+]]
        [medley.core])
  (:require [clojure.edn :as edn]
            [bouncer [core :as b] [validators :as v]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [filevents.core :refer [watch]]
            [me.raynes.fs :as fs]))

(timbre/refer-timbre)

(def props
  "A ref for storing the combined configuration properties."
  (ref nil))

(def cmd-line-props
  "A ref for storing the props passed in on the command-line."
  (ref nil))

(def filters
  [[:irods-user]
   [:irods-password]])

(defn pprint-to-string
  [m]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (clojure.pprint/pprint m))
    (str sw)))

(v/defvalidator stringv
  {:default-message-format "%s must be a string", :optional true}
  [s]
  (string? s))

(v/defvalidator keywordv
  {:default-message-format "%s must be a keyword", :optional true}
  [k]
  (keyword? k))

(def cfgv
  {:port           [v/required v/number]
   :irods-host     [v/required stringv]
   :irods-port     [v/required stringv]
   :irods-zone     [v/required stringv]
   :irods-home     [v/required stringv]
   :irods-user     [v/required stringv]
   :irods-password [v/required stringv]
   :anon-user      [v/required stringv]
   :log-file       stringv
   :log-size       v/number
   :log-backlog    v/number
   :log-level      keywordv})

(def defaults
  {:log-level   :info
   :log-size    (* 100 1024 1024)
   :log-backlog 10})

(defn valid-config?
  [cfg]
  (let [errs (first (b/validate cfg cfgv))]
    (when errs
      (error (pprint-to-string errs)))
    (not errs)))

(defn loggable-config
  [cfg]
  (let [bad-keys #{:irods-password}]
    (pprint-to-string
     (filter-keys #(not (contains? bad-keys %)) cfg))))

(defn configure-logging
  []
  (when (:log-level @props)
    (timbre/set-level! (:log-level @props)))
  (when (:log-file @props)
    (timbre/set-config! [:appenders :rotor]
                        {:enabled? true
                         :async? false
                         :max-messages-per-msecs nil
                         :fn rotor/appender-fn})
    (timbre/set-config! [:shared-appender-config :rotor]
                        {:path     (:log-file @props)
                         :max-size (:log-size @props)
                         :backlog  (:log-backlog @props)})))


(defn load-config-from-file
  "Loads the configuration settings from a file."
  []
  (let [cfg (edn/read-string (slurp (:config @cmd-line-props)))]
    (when-not (valid-config? cfg)
      (error "Config file has errors, exiting.")
      (System/exit 1))
    (dosync (ref-set props (merge defaults cfg @cmd-line-props)))
    (configure-logging)
    (info "Config file settings:\n" (loggable-config cfg))
    (info "Command-line settings:\n" (loggable-config @cmd-line-props))
    (info "Combined settings:\n" (loggable-config @props))))

(defmulti watch-handler
  (fn [event path] event))

(defmethod watch-handler :created
  [event path]
  (warn path "was created. Attempting to load as config.")
  (load-config-from-file))

(defmethod watch-handler :deleted
  [event path]
  (warn path "was deleted! Bad things may happen!"))

(defmethod watch-handler :modified
  [event path]
  (info path "was modified, reloading config.")
  (load-config-from-file))

(defn watch-config
  []
  (watch watch-handler (:config @cmd-line-props)))

(defn load-config
  [options]
  (dosync (ref-set cmd-line-props options))
  (load-config-from-file)
  (watch-config))


