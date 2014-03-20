(use '[leiningen.exec :only (deps)]
     '[leiningen.core.project :only (defproject)])
(deps '[[cheshire "5.3.1"]
        [me.raynes/fs "1.4.4"]])

(require '[clojure.java.shell :as sh]
         '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.pprint]
         '[me.raynes.fs :as fs]
         '[clojure.string :as string])

(import '[java.nio.file Files])


(defn path-join
  "Joins paths together and returns the resulting path as a string. nil and empty strings are
   silently discarded.

   Parameters:
     paths - a parameter list of names to join together to form a path.

   Returns:
     It returns the path as a string."
  [& paths]
  (let [paths' (remove empty? paths)]
    (if (empty? paths')
      ""
      (str (apply io/file paths')))))


;;; Maps the internal dependencies to their directory in the checkout.
(def lib-dep-dir-map
  {"clj-cas"          "libs/clj-cas"
   "clj-icat-direct"  "libs/clj-icat-direct"
   "clj-jargon"       "libs/clj-jargon"
   "de-authn"         "libs/de-authn"
   "de-persistence"   "libs/de-persistence"
   "heuristomancer"   "libs/heuristomancer"
   "hibernatetoolkit" "libs/hibernatetoolkit"
   "clojure-commons"  "libs/iplant-clojure-commons"
   "kameleon"         "libs/kameleon"
   "mescal"           "libs/mescal"
   "metadactyl"       "libs/metadactyl"})


;;; Order that the libs should be built in.
(def libs-build-order
  ["hibernatetoolkit"
   "de-authn"
   "de-persistence"
   "metadactyl"
   "iplant-clojure-commons"
   "kameleon"
   "clj-cas"
   "clj-jargon"
   "clj-icat-direct"
   "heuristomancer"])


(defn project-deps
  "Returns a vector containing a projects dependencies"
  [path-to-project-file]
  (:dependencies (apply hash-map (drop 3 (read-string (slurp path-to-project-file))))))


(defn iplant-deps
  "Returns the iplant dependencies from a list of dependencies"
  [dep-list]
  (filter #(.startsWith (str (first %1)) "org.iplantc") dep-list))


(defn project-dirs-from-dep
  "Returns the project directory associated with the provided dep"
  [dep-list]
  (map #(get lib-dep-dir-map (name (first %1))) dep-list))


(defn- find-folders
  [filename]
  (map #(str (.getParentFile %1))
       (filter #(not (.contains (str %1) "checkouts/"))
               (filter #(.endsWith (str %1) filename)
                       (file-seq (io/file "."))))))


(def clojure-project-dirs (find-folders "/project.clj"))
(def java-project-dirs (find-folders "/pom.xml"))
(def checkout-dir "checkouts")


(defn delete-checkout-dirs
  [paths]
  (doseq [pdir paths]
    (fs/with-cwd pdir
      (when (and (fs/exists? checkout-dir)
                 (fs/directory? checkout-dir))
        (fs/delete-dir checkout-dir)))))


(defn create-checkout-dirs
  [paths]
  (doseq [pdir paths]
    (fs/with-cwd pdir
      (when-not (fs/exists? checkout-dir)
        (fs/mkdir checkout-dir)))))


(defn print-shell-result
  [result]
  (when-not (zero? (:exit result))
    (println "exit: " (:exit result)))
  (when-not (string/blank? (:out result))
    (println "stdout:")
    (println (:out result)))
  (when-not (string/blank? (:err result))
    (println "error:")
    (println (:err result)))
  (when-not (zero? (:exit result))
    (println "ERROR ENCOUNTERED, EXITING!!!")
    (System/exit 1)))


(defn create-checkout-symlinks
  [project-path]
  (fs/with-cwd project-path
    (let [project-clj (path-join project-path "project.clj")
          symlinks    (-> project-clj project-deps iplant-deps project-dirs-from-dep)]
      (if-not (fs/exists? checkout-dir)
        (fs/mkdir checkout-dir))
      (doseq [symlink symlinks]
        (let [target-path (str "../../../" symlink)
              link-path   (fs/base-name target-path)]
          (println ">> Setting symlink " (path-join checkout-dir link-path) " to " target-path)
          (sh/with-sh-dir (path-join project-path checkout-dir)
            (print-shell-result (sh/sh "ln" "-sf" target-path link-path))))))))


(defn clojure-project?
  [path-to-project]
  (fs/exists? (path-join (str path-to-project) "project.clj")))


(defn java-project?
  [path-to-project]
  (fs/exists? (path-join (str path-to-project) "pom.xml")))


(defn install-clojure-project
  [path-to-project]
  (sh/with-sh-dir path-to-project
    (println ">> Installing " path-to-project)
    (print-shell-result (sh/sh "lein" "clean"))
    (print-shell-result (sh/sh "lein" "install"))))


(defn install-java-project
  [path-to-project]
  (sh/with-sh-dir path-to-project
    (println ">> Installing " path-to-project)
    (print-shell-result (sh/sh "mvn" "clean"))
    (print-shell-result (sh/sh "mvn" "install"))))


(defn install-lein-plugins
  "Installs the lein plugins from the lein-plugins directory."
  []
  (let [dirs (fs/list-dir "lein-plugins")]
    (println "> Installing lein plugins")
    (doseq [d dirs]
      (install-clojure-project (path-join "lein-plugins" (str d))))
    (println "")))


(defn build-clojure-project
  "Builds a clojure project"
  [path-to-project]
  (sh/with-sh-dir path-to-project
    (println ">> Building " path-to-project)
    (print-shell-result (sh/sh "lein" "clean"))
    (print-shell-result (sh/sh "lein" "uberjar"))))


(defn build-java-project
  [path-to-project]
  (sh/with-sh-dir path-to-project
    (println ">> Installing " path-to-project)
    (print-shell-result (sh/sh "mvn" "clean"))
    (print-shell-result (sh/sh "mvn" "build"))))


(defn install-libs
  "Installs the libs"
  []
  (println "> Installing libs")
  (doseq [d libs-build-order]
    (let [project-path (path-join "libs" (str d))]
      (cond
       (clojure-project? project-path)
       (install-clojure-project project-path)

       (java-project? project-path)
       (install-java-project project-path)

       :else (println ">> Don't know how to build this."))
      (println ""))))


(defn build-database-func
  [path-to-project]
  (sh/with-sh-dir path-to-project
    (println ">> Building database " path-to-project)
    (print-shell-result (sh/sh "./build.sh"))))


(defn build-databases
  []
  (let [dirs (fs/list-dir "databases")]
    (println "> Building the databases")
    (doseq [d dirs] (build-database-func (path-join "databases" (str d))))))


(defn uberjar-func
  [d]
  (let [project-path (path-join "services" (str d))]
    (cond
      (clojure-project? project-path)
      (build-clojure-project project-path)

      (java-project? project-path)
      (build-java-project project-path)

      :else (println ">> Don't know how to build this."))
      (println "")))


(defn uberjar-services
  "Uberjars up the services"
  []
  (let [dirs (fs/list-dir "services")]
    (println "> Uberjar'ing the services")
    (doseq [d dirs] (uberjar-func d))))


(defn uberjar-tools
  "Uberjars up the tools"
  []
  (let [dirs (fs/list-dir "tools")]
    (println "> Uberjar'ing the tools")
    (doseq [d dirs] (uberjar-func d))))


(doseq [proj clojure-project-dirs]
  (println "> Handling " proj)
  (create-checkout-symlinks proj)
  (println ""))
(install-lein-plugins)
(install-libs)
(uberjar-services)
(uberjar-tools)
(build-databases)

