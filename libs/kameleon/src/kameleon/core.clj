(ns kameleon.core
  (:import [java.io InputStreamReader PushbackReader]
           [java.net URL]))

(def ^:private project-clj-location
  "The location of the project.clj file within the JAR file."
  "/META-INF/leiningen/org.iplantc/kameleon/project.clj")

(defn- get-location
  "Gets the location of the source file containing this source file."
  []
  (.. (class get-location)
      getProtectionDomain
      getCodeSource
      getLocation))

(defn- read-project-clj
  "Reads the project.clj file from the JAR file containing this source file."
  []
  (let [location (get-location)]
    (when-not (nil? location)
      (-> (str "jar:" location "!" project-clj-location)
          (URL.)
          (.openStream)
          (InputStreamReader.)
          (PushbackReader.)
          (read)))))

(defn compatible-db-version
  "Retrieves the database version that the current version of Kameleon is
   compatible with."
  []
  (let [attrs (read-project-clj)]
    (get-in (into {} (map vec (partition 2 (drop 3 attrs))))
            [:manifest "db-version"])))
