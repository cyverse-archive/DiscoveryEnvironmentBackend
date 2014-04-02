(ns clojure-commons.props
  (:use [clojure.java.io :only (file input-stream)])
  (:import [java.net URLDecoder]
           [org.apache.commons.configuration PropertiesConfiguration]))

(defn read-properties
  "Reads in properties from a file and instantiates a loaded Properties object.
   Adapted from code in the clojure.contrib.properties."
  [file-path]
  (with-open [f (java.io.FileInputStream. (file file-path))]
    (doto (java.util.Properties.)
      (.load f))))

(defn find-properties-file
  "Searches the classpath for the named properties file."
  [prop-name]
  (let [resource (.getResource (.. Thread currentThread getContextClassLoader) prop-name)]
    (when-not (nil? resource) (.getFile resource))))

(defn find-resources-file
  [filename]
  (find-properties-file filename))

(defn- find-config-file
  "Finds a configuration file, which may in tghe classpath or in the 'resources'
   subdirectory of the current working directory."
  [file-name]
  (let [prop-path (find-properties-file file-name)]
    (if (nil? prop-path) (str "resources/" file-name) prop-path)))

(defn parse-properties
  [file-name]
  (read-properties (URLDecoder/decode (find-config-file file-name))))

(defn load-properties-configuration
  "Loads a configuration from a file using Apache's Commons Configuration
   library."
  [file-name]
  (PropertiesConfiguration. (find-config-file file-name)))
