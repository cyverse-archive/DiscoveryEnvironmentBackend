(ns fishy.core
  (:gen-class)
  (:use [clojure.java.io :only [file]])
  (:require [fishy.util.config :as config]))

(def ^:private default-conf-dir "/etc/iplant/de")

(defn- iplant-conf-dir-file
  [filename]
  (let [conf-dir (or (System/getenv "IPLANT_CONF_DIR") default-conf-dir)]
    (let [f (file conf-dir filename)]
      (when (.isFile f) (.getPath f)))))

(defn- cwd-file
  [filename]
  (let [f (file filename)]
    (when (.isFile f) (.getPath f))))

(defn- classpath-file
  [filename]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.findResource filename)
      (.toURI)
      (file)))

(defn- no-configuration-found
  [filename]
  (throw (RuntimeException. (str "configuration file " filename " not found"))))

(defn- find-config-file
  []
  (let [conf-file "fishy.properties"]
    (or (iplant-conf-dir-file conf-file)
        (cwd-file conf-file)
        (classpath-file conf-file)
        (no-configuration-found conf-file))))

(defn load-config-from-file
  ([]
     (load-config-from-file (find-config-file)))
  ([config-path]
     (config/load-config-from-file config-path)))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))
