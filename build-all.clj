(use '[leiningen.exec :only (deps)]
     '[leiningen.core.project :only (defproject)])
(deps '[[cheshire "5.3.1"]
        [me.raynes/fs "1.4.4"]
        [org.clojure/tools.cli "0.3.1"]
        [org.clojure/clojure "1.5.1"]])

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
  ["de-base-services-pom"
   "hibernatetoolkit"
   "de-authn"
   "de-persistence"
   "metadactyl"
   "iplant-clojure-commons"
   "kameleon"
   "clj-cas"
   "clj-jargon"
   "clj-icat-direct"
   "mescal"
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


(def cmdtar-projects
  #{"clavin"
    "facepalm"
    "proboscis"})


(defn build-clojure-project
  "Builds a clojure project"
  [path-to-project]
  (sh/with-sh-dir path-to-project
    (println ">> Building " path-to-project)
    (print-shell-result (sh/sh "lein" "clean"))
    (print-shell-result (sh/sh "lein" "uberjar"))
    (if (contains? cmdtar-projects (fs/base-name path-to-project))
      (print-shell-result (sh/sh "lein" "iplant-cmdtar")))))


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
  [project-path]
  (cond
    (clojure-project? project-path)
    (build-clojure-project project-path)

    (java-project? project-path)
    (build-java-project project-path)

    :else (println ">> Don't know how to build this."))
    (println ""))


(defn rpm-func
  [project-path build-num]
  (if (fs/exists? (path-join project-path "project.clj"))
    (println ">> Generating RPM for " project-path " with a build number of " build-num)
      (sh/with-sh-dir project-path
        (print-shell-result
          (sh/sh "lein" "iplant-rpm" build-num)))))


(defn uberjar-services
  "Uberjars up the services"
  []
  (let [dirs  (fs/list-dir "services")
        rpms? (System/getenv "BUILD_RPMS")
        bnum  (System/getenv "BUILD_NUMBER")]
    (println "> Uberjar'ing the services")
    (doseq [d dirs]
      (uberjar-func (path-join "services" (str d)))
      (when rpms?
        (when-not bnum
          (println "ERROR: BUILD_RPMS environment variable is set, but the BUILD_NUMBER is missing.")
          (System/exit 1))
        (rpm-func (path-join "services" (str d)) bnum)))))


(defn rpm-func
  [project-path]
  (if (fs/exists? (path-join project-path "project.clj"))
    (println ">> Generating RPM for " project-path)
    (sh/with-sh-dir project-path
      (print-shell-result
       (sh/sh "lein" "iplant-rpm" (System/getenv "BUILD_NUMBER"))))))


(defn uberjar-tools
  "Uberjars up the tools"
  []
  (let [dirs  (fs/list-dir "tools")
        rpms? (System/getenv "BUILD_RPMS")
        bnum  (System/getenv "BUILD_NUMBER")]
    (println "> Uberjar'ing the tools")
    (doseq [d dirs]
      (uberjar-func (path-join "tools" (str d)))
      (when rpms?
        (when-not bnum
          (println "ERROR: BUILD_RPMS environment variable is set, but the BUILD_NUMBER is missing.")
          (System/exit 1))
        (rpm-func (path-join "tools" (str d)) bnum)))))


(defn bash-cmd
  [str-to-run]
  (sh/sh "bash" "-c" str-to-run))

(defn move-builds
  [path-to-project]
  (let [target-path (path-join path-to-project "target")]
    (when (fs/exists? target-path)
      (println ">> Copying builds from " target-path " to builds directory.")
      (print-shell-result (bash-cmd (str "mv " target-path "/*.jar " "builds"))))
    (when (System/getenv "BUILD_RPMS")
      (println ">> Copying any RPMs from " path-to-project " to builds directory.")
      (print-shell-result (bash-cmd (str "mv " path-to-project "/*.rpm " "builds"))))
    (when (contains? cmdtar-projects (fs/base-name path-to-project))
      (println ">> Copying any cmdtars from " path-to-project " to builds directory.")
      (print-shell-result (bash-cmd (str "mv " target-path "/*.tar.gz " "builds"))))
    (println "")))


(defn move-database
  [path-to-project]
  (when (fs/exists? path-to-project)
    (println ">> Copying builds from " path-to-project " to builds directory.")
    (print-shell-result (sh/sh "bash" "-c" (str "mv " path-to-project "/*.tar.gz " "builds")))
  (println "")))


(defn archive-builds
  []
  (println "> Moving builds to builds directory.")
  (when (fs/exists? "builds")
    (println ">> Cleaning out builds directory")
    (print-shell-result (sh/sh "rm" "-r" "builds")))
  (println ">> Creating builds directory")
  (print-shell-result (sh/sh "mkdir" "builds"))
  (let [move-it #(doseq [proj (fs/list-dir %1)]
                   (move-builds (path-join %1 (str proj))))]
    (move-it "lein-plugins")
    (move-it "libs")
    (move-it "services")
    (move-it "tools")
    (doseq [proj (fs/list-dir "databases")]
      (move-database (path-join "databases" (str proj))))))


(defn do-symlinks
  []
  (doseq [proj clojure-project-dirs]
    (println "> Handling " proj)
    (create-checkout-symlinks proj)
    (println "")))

(defn do-everything
  []
  (do-symlinks)
  (install-lein-plugins)
  (install-libs)
  (uberjar-services)
  (uberjar-tools)
  (build-databases))

(defn main-func
  []
  (let [args *command-line-args*
        opts ["help"
              "symlinks"
              "lein-plugins"
              "libs"
              "services"
              "tools"
              "databases"]
        usage (str "These are the options: " opts)]
    (when (> (count args) 2)
      (println "Too many args!")
      (println usage))
    (when-not (contains? (set opts) (second args))
      (println "lol nope")
      (println usage))
    (case (second args)
      "help"         (println usage)
      "symlinks"     (do-symlinks)
      "lein-plugins" (install-lein-plugins)
      "libs"         (install-libs)
      "services"     (uberjar-services)
      "tools"        (uberjar-tools)
      "databases"    (build-databases)
      (do-everything))
    (when-not (= (second args) "symlinks")
      (archive-builds))))


(main-func)
