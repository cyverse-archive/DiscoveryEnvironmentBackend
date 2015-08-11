(use '[leiningen.exec :only (deps)]
     '[leiningen.core.project :only (defproject)])
(deps '[[cheshire "5.3.1"]
        [me.raynes/fs "1.4.6"]
        [org.clojure/tools.cli "0.3.1"]
        [org.clojure/clojure "1.5.1"]
        [medley "0.1.5"]
        [org.clojure/tools.cli "0.2.1"]])

(require '[clojure.java.shell :as sh]
         '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.pprint]
         '[me.raynes.fs :as fs]
         '[clojure.string :as string]
         '[clojure.pprint :as pprint]
         '[clojure.tools.cli :as cli]
         '[clojure.edn :as edn])

(use '[medley.core])

(import '[java.nio.file Files])

(def projects (edn/read-string (slurp "project-defs.edn")))

; Categorized by type
(def lein-plugins (filter-vals #(= (:type %) :lein-plugin) projects))
(def tools        (filter-vals #(= (:type %) :tool) projects))
(def services     (filter-vals #(= (:type %) :service) projects))
(def libs         (filter-vals #(= (:type %) :library) projects))
(def dbs          (filter-vals #(= (:type %) :database) projects))


; Categorized by programming language
(def clojure-projects (filter-vals #(= (:language %) :clojure) projects))
(def java-projects    (filter-vals #(= (:lanugage %) :java) projects))


; libraries are the only thing that have to be built in a certain order.
(def libs-build-order (sort-by #(:type-num (get libs %)) (keys libs)))


; cmdtar projects are an exceptional case
(def cmdtar           (filter-vals :tarball? projects))
(def uberjar-projects (filter-vals :uberjar? projects))


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


(defn print-shell-result
  [result]
  (when-not (zero? (:exit result))
    (println "exit:" (:exit result)))
  (when-not (string/blank? (:out result))
    (println "stdout:")
    (println (:out result)))
  (when-not (string/blank? (:err result))
    (println "error:")
    (println (:err result)))
  (when-not (zero? (:exit result))
    (println "ERROR ENCOUNTERED, EXITING!!!")
    (System/exit 1)))


;;; Multi-methods for installations
;;; The dispatch function extracts the :install field from the
;;; project map.
(defmulti install
  (fn [project-map]
    (:install project-map)))

(defmethod install :lein
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Installing" path-to-project)
      (print-shell-result (sh/sh "lein" "clean"))
      (print-shell-result (sh/sh "lein" "install")))))


(defmethod install :mvn
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Installing" path-to-project)
      (print-shell-result (sh/sh "mvn" "clean"))
      (print-shell-result (sh/sh "mvn" "install")))))


(defmethod install :default
  [project-map]
  (println ">> Don't know how to install" (:path project-map)))


;;; Multi-methods for builds
;;; The dispatch function extracts the :build field from the
;;; project map.
(defmulti build
  (fn [project-map]
    (:build project-map)))

(defmethod build :lein
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Building" path-to-project)
      (print-shell-result (sh/sh "lein" "clean"))
      (print-shell-result (sh/sh "lein" "uberjar")))))


(defmethod build :kifshare
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir
     path-to-project
     (println ">> Building" path-to-project)
     (print-shell-result (sh/sh "grunt" "--version"))
     (print-shell-result (sh/sh "npm" "--version"))
     (print-shell-result (sh/sh "npm" "install"))
     (print-shell-result (sh/sh "grunt" "clean-all"))
     (print-shell-result (sh/sh "lein" "uberjar")))))


(defmethod build :cmdtar
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Building iplant-cmdtar project" path-to-project)
      (print-shell-result (sh/sh "lein" "clean"))
      (print-shell-result (sh/sh "lein" "uberjar"))
      (print-shell-result (sh/sh "lein" "iplant-cmdtar")))))


(defmethod build :mvn
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Building Java project" path-to-project)
      (print-shell-result (sh/sh "mvn" "clean"))
      (print-shell-result (sh/sh "mvn" "build")))))


(defmethod build :shell
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Building shell project" path-to-project)
      (print-shell-result (sh/sh "./build.sh")))))


(defmethod build :default
  [project-map]
  (println ">> Don't know how to build" (:path project-map)))


(defn install-libs
  "Installs the libraries in the correct order."
  []
  (println "> Installing libs")
  (doseq [prj-name libs-build-order]
    (install (get projects prj-name))
    (println "")))


(defn install-lein-plugins
  []
  (println "> Installing the lein-plugins")
  (doseq [[prj-name prj-map] (seq lein-plugins)]
    (install prj-map)))


(defn- build-projects
  [projects]
  (doseq [k (keys projects)]
    (build (get projects k)))
  (println ""))


(defn build-databases
  []
  (println "> Building the databases")
  (build-projects dbs))


(defn build-services
  []
  (println "> Building the services")
  (build-projects services))


(defn build-tools
  []
  (println "> Building tools")
  (build-projects tools))


(defn build-libs
  []
  (println "> Building libraries")
  (doseq [prj (mapv #(get libs %) libs-build-order)]
    (build-projects prj)))


(defn bash-cmd
  "A utility function for execing a script with bash."
  [str-to-run]
  (sh/sh "bash" "-c" str-to-run))


(defn move-uberjar-build
  "Moves all .jar files from the target/ directory of the given
   project. Yeah, it'll blow up if it's not there, but that's
   intentional. We don't want any false positive builds."
  [project-map]
  (let [target-path (path-join (:path project-map) "target")]
    (println ">> Copying builds from" target-path "to builds directory.")
    (print-shell-result (bash-cmd (str "mv " target-path "/*.jar " "builds")))))


(defn move-cmdtar-build
  "Moves all .tar.gz files found in the project's target directory."
  [project-map]
  (let [path-to-project (:path project-map)
        target-path     (path-join path-to-project "target")]
    (println ">> Copying any cmdtars from" path-to-project "to builds directory.")
    (print-shell-result (bash-cmd (str "mv " target-path "/*.tar.gz " "builds")))))


(defn move-database-build
  "Moves all .tar.gz files found in the project's root directory."
  [project-map]
  (let [path-to-project (:path project-map)]
    (println ">> Copying builds from" path-to-project "to builds directory.")
    (print-shell-result (sh/sh "bash" "-c" (str "mv " path-to-project "/*.tar.gz " "builds")))))


(defn- archive-project
  [opts project-map]
  (when (:archive? project-map)
    (when (:tarball? project-map)
      (move-cmdtar-build project-map))

    (when (:uberjar? project-map)
      (move-uberjar-build project-map))

    (when (= (:type project-map) :database)
      (move-database-build project-map))))


(defn- archive-all
  "Iterates over all of the projects defined in the given map
   and passes the project map to the (archive-project) function."
  [opts projects-map]
  (doseq [[project-name project-map] (seq projects-map)]
    (archive-project opts project-map)))


(defn archive-services
  [opts]
  (println "> Moving services builds")
  (archive-all opts services)
  (println ""))


(defn archive-lein-plugins
  [opts]
  (println "> Moving lein-plugins builds")
  (archive-all opts lein-plugins)
  (println ""))


(defn archive-libs
  [opts]
  (println "> Moving library builds")
  (archive-all opts libs)
  (println ""))


(defn archive-databases
  [opts]
  (println "> Moving database builds")
  (archive-all opts dbs)
  (println ""))


(defn archive-tools
  [opts]
  (println "> Moving tool builds")
  (archive-all opts tools)
  (println ""))


(defn prep-builds-dir
  "Deletes and recreates the top-level builds/ directory, as
   necessary."
  []
  (when (fs/exists? "builds")
    (println ">> Cleaning out builds directory")
    (print-shell-result (sh/sh "rm" "-r" "builds")))
  (println ">> Creating builds directory")
  (print-shell-result (sh/sh "mkdir" "builds")))


(defn do-libs
  [opts]
  (install-libs)
  (if (:archive opts)
    (archive-libs opts)))


(defn do-lein-plugins
  [opts]
  (install-lein-plugins)
  (if (:archive opts)
    (archive-lein-plugins opts)))


(defn do-services
  [opts]
  (build-services)
  (if (:archive opts)
    (archive-services opts)))


(defn do-tools
  [opts]
  (build-tools)
  (if (:archive opts)
    (archive-tools opts)))


(defn do-databases
  [opts]
  (build-databases)
  (if (:archive opts)
    (archive-databases opts)))


(defn do-everything
  [opts]
  (do-lein-plugins opts)
  (do-libs opts)
  (do-services opts)
  (do-tools opts)
  (do-databases opts))


(defn parse-args
  [args]
  (cli/cli
   args
   ["-h" "--help" "Show help." :default false :flag true]
   ["-b" "--build-number" "Assigns a build number" :default nil]
   ["-a" "--archive" "Archive builds?" :default false :flag true]))


(def valid-cmds
  #{"help"
    "lein-plugins"
    "libs"
    "services"
    "tools"
    "databases"})

(defn validate-cmds
  [cmds]
  (let [invalid-cmds (filter #(not (contains? valid-cmds %)) cmds)]
    (when (pos? (count invalid-cmds))
      (println "The following are invalid commands:")
      (doseq [cmd invalid-cmds]
        (println "*" cmd))
      (System/exit 1))))

(defn main-func
  []
  (let [args *command-line-args*
        [opts cmds help-str] (parse-args args)
        cmds        (drop 1 cmds)
        _           (validate-cmds cmds)]
    (when (:archive opts)
      (prep-builds-dir))

    (when (pos? (count cmds))
      (doseq [cmd cmds]
        (case cmd
          "help"         (do (println help-str)
                           (println "Commands are:" (string/join " " (seq valid-cmds)))
                           (System/exit 0))
          "lein-plugins" (do-lein-plugins opts)
          "libs"         (do-libs opts)
          "services"     (do-services opts)
          "tools"        (do-tools opts)
          "databases"    (do-databases opts)
          (println "Not a valid command:" cmd))))

    (when-not (pos? (count cmds))
      (do-everything opts))))


(main-func)
