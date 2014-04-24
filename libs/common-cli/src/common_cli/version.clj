(ns common-cli.version
  (:require [trptcolin.versioneer.core :as version]))

(defn jar-domain
  []
  (.. (class *ns*)
      (getProtectionDomain)
      (getCodeSource)
      (getLocation)))

(defn jar-file-stream
  [path-in-jar]
  (.openStream
   (java.net.URL.
    (str "jar:"
         (jar-domain)
         path-in-jar))))

(defn slurp-manifest
  []
  (slurp (jar-file-stream "!/META-INF/MANIFEST.MF")))

(defn manifest
  []
  (or (slurp-manifest) ""))

(defn version-info
  [group-id artifact-id]
  (str "App-Version: " (version/get-version group-id artifact-id) "\n"
       (manifest)))
