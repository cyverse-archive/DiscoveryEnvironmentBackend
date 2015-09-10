(ns data-info.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [data-info.util.config :as config]
            [clojure.tools.nrepl.server :as nrepl]
            [me.raynes.fs :as fs]
            [common-cli.core :as ccli]
            [service-logging.thread-context :as tc]
            [data-info.services.icat :as icat]))


(defn- start-nrepl
  []
  (nrepl/start-server :port 7888))


(defn- iplant-conf-dir-file
  [filename]
  (when-let [conf-dir (System/getenv "IPLANT_CONF_DIR")]
    (let [f (io/file conf-dir filename)]
      (when (.isFile f) (.getPath f)))))


(defn- cwd-file
  [filename]
  (let [f (io/file filename)]
    (when (.isFile f) (.getPath f))))


(defn- classpath-file
  [filename]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.findResource filename)
      (.toURI)
      io/file))


(defn- no-configuration-found
  [filename]
  (throw (RuntimeException. (str "configuration file " filename " not found"))))


(defn- find-configuration-file
  []
  (let [conf-file "data-info.properties"]
    (or (iplant-conf-dir-file conf-file)
        (cwd-file conf-file)
        (classpath-file conf-file)
        (no-configuration-found conf-file))))


(defn- load-configuration-from-file
  "Loads the configuration properties from a file."
  ([]
   (load-configuration-from-file (find-configuration-file)))

  ([path]
    (config/load-config-from-file path)))


(defn lein-ring-init
  []
  (load-configuration-from-file)
  (icat/configure-icat)
  (start-nrepl))


(defn repl-init
  []
  (load-configuration-from-file)
  (icat/configure-icat))


(defn- cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/data-info.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn -main
  [& args]
  (tc/with-logging-context config/svc-info
    (require 'data-info.routes)
    (let [app (eval 'data-info.routes/app)
          {:keys [options arguments errors summary]} (ccli/handle-args config/svc-info
                                                                       args
                                                                       cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 (str "The config file does not exist.")))
      (when-not (fs/readable? (:config options))
        (ccli/exit 1 "The config file is not readable."))
      (load-configuration-from-file (:config options))
      (icat/configure-icat)
      (jetty/run-jetty app {:port (config/listen-port)}))))
