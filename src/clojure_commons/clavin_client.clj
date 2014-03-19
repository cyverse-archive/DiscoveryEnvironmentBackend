(ns clojure-commons.clavin-client
  (:require [clojure.string :as string] 
            [clojure-commons.file-utils :as ft] 
            [zookeeper :as zk]
            [zookeeper.data :as data]))

(def ^:dynamic zkcl nil)

(defmacro with-zk
  [conn-str & body]
  `(let [zconns# ~conn-str
         cl#    (zk/connect zconns#)]
     (binding [zkcl cl#]
       (try (do ~@body)
         (finally (zk/close zkcl))))))

(defn read-node
  "Reads the bytes from a node and returns them as a string."
  [npath]
  (let [node-data (:data (zk/data zkcl npath))]
    (if (nil? node-data)
      ""
      (data/to-string node-data))))

(defn list-children
  [npath]
  (zk/children zkcl npath))

(defn deployment
  []
  (let [local-host (java.net.InetAddress/getLocalHost)
        local-ip   (.getHostAddress local-host)
        dep-path   (ft/path-join "/hosts" local-ip)]
    (first (filter #(not= % "admin") (list-children dep-path)))))

(defn can-run?
  []
  (deployment))

(defn properties
  [svc]
  (let [dpmt      (deployment)
        root-join (partial ft/path-join "/")
        root-dep  (apply root-join (string/split dpmt #"\."))
        dpmt-path (ft/path-join root-dep svc)]
    (apply merge 
           (for [pk (list-children dpmt-path)]
             {pk (read-node (ft/path-join dpmt-path pk))}))))

