(ns leiningen.iplant-cmdtar
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [join]]
        [fleet]
        [leiningen.core.eval :only [sh]])
  (:require [leiningen.core.main :as main]
            [leiningen.uberjar :as uberjar]))

(def ^:private cmd-template
  "The template used to generate the command file."
  "cmd-template.fleet")

(defn- slurp-resource
  "Slurps the contents of a resource that can be found relative to a location
   on the classpath."
  [resource-path]
  (let [loader (.. (Thread/currentThread) getContextClassLoader)]
    (slurp (.getResourceAsStream loader resource-path))))

(defn- load-template
  "Loads a Fleet template from a template file that is located relative to a
   location on the classpath."
  [template-path]
  (fleet [spec] (slurp-resource template-path) {:escaping :bypass}))

(defn- gen-file
  "Generates a file with the given name using the given template name."
  [settings f]
  (spit f (str ((load-template cmd-template) settings))))

(defn- make-executable
  "Ensures that a file is executable by everyone."
  [f]
  (.setExecutable f true false)
  (.setReadable f true false))

(defn- exec
  "Executes a command, throwing an exception if the command fails."
  [& args]
  (let [status (apply sh args)]
    (when (not= status 0)
      (let [cmd (join " " args)]
        (throw (Exception. (str cmd " failed with status " status)))))))

(defn- build-uberjar
  "Calls the uberjar task in order to build the JAR file that will be included
   in the tarball."
  [project]
  (try
    (uberjar/uberjar project)
    (catch Exception e
      (main/abort "iplant-cmdtar aborting because uberjar creation failed."))))

(defn iplant-cmdtar
  "Generates a binary tarball consisting of an uberjar along with a wrapper
   script."
  [project]
  (let [target-name  "target"
        target-dir   (file target-name)
        exec-name    (:name project)
        exec-file    (file target-dir exec-name)
        tarball-name (str target-name "/" exec-name ".tar.gz")
        jar-name     (.getName (file (build-uberjar project)))]
    (try (gen-file project exec-file)
         (make-executable exec-file)
         (exec "tar" "czvf" tarball-name "-C" target-name exec-name jar-name)
         (main/info "Created" (.getAbsolutePath (file tarball-name)))
         (catch Exception e
           (main/abort "iplant-cmdtar tarball creation failed:"
                       (.getMessage e))))
    tarball-name))
