(use '[leiningen.exec :only (deps)]
     '[leiningen.core.project :only (defproject)])
(deps '[[cheshire "5.3.1"]
        [me.raynes/fs "1.4.4"]
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
         '[clojure.tools.cli :as cli])

(use '[medley.core])

(import '[java.nio.file Files])


(def projects
  {"lein-iplant-cmdtar"
   {:type     :lein-plugin
    :path     "lein-plugins/lein-iplant-cmdtar"
    :build    :lein
    :install  :lein
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :clojure
    :archive? false}

   "lein-iplant-rpm"
   {:type     :lein-plugin
    :path     "lein-plugins/lein-iplant-rpm"
    :build    :lein
    :install  :lein
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :clojure
    :archive? false}

   "clj-cas"
   {:type     :library
    :type-num 7
    :path     "libs/clj-cas"
    :build    :lein
    :install  :lein
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :clojure
    :archive? false}

   "clj-icat-direct"
   {:type     :library
    :type-num 9
    :path     "libs/clj-icat-direct"
    :build    :lein
    :install  :lein
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :clojure
    :archive? false}

   "clj-jargon"
   {:type     :library
    :type-num 8
    :path     "libs/clj-jargon"
    :build    :lein
    :install  :lein
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :clojure
    :archive? false}

   "de-authn"
   {:type     :library
    :type-num 2
    :path     "libs/de-authn"
    :build    :mvn
    :install  :mvn
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :java
    :archive? false}

   "de-base-services-pom"
   {:type     :library
    :type-num 0
    :path     "libs/de-base-services-pom"
    :build    :mvn
    :install  :mvn
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :java
    :archive? false}

   "de-persistence"
   {:type     :library
    :type-num 3
    :path     "libs/de-persistence"
    :build    :mvn
    :install  :mvn
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :java
    :archive? false}

   "heuristomancer"
   {:type     :library
    :type-num 11
    :path     "libs/heuristomancer"
    :build    :lein
    :install  :lein
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :clojure
    :archive? false}

   "hibernatetoolkit"
   {:type     :library
    :type-num 1
    :path     "libs/hibernatetoolkit"
    :build    :mvn
    :install  :mvn
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :java
    :archive? false}

   "iplant-clojure-commons"
   {:type     :library
    :type-num 5
    :path     "libs/iplant-clojure-commons"
    :build    :lein
    :install  :lein
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :clojure
    :archive? false}

   "kameleon"
   {:type     :library
    :type-num 6
    :path     "libs/kameleon"
    :build    :lein
    :install  :lein
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :clojure
    :archive? false}

   "mescal"
   {:type     :library
    :type-num 10
    :path     "libs/mescal"
    :build    :lein
    :install  :lein
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :clojure
    :archive? false}

   "metadactyl"
   {:type     :library
    :type-num 4
    :path     "libs/metadactyl"
    :build    :mvn
    :install  :mvn
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? true
    :language :java
    :archive? false}

   "clockwork"
   {:type     :service
    :path     "services/clockwork"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "Conrad"
   {:type     :service
    :path     "services/Conrad"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "dewey"
   {:type     :service
    :path     "services/dewey"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "Donkey"
   {:type     :service
    :path     "services/Donkey"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "Infosquito"
   {:type     :service
    :path     "services/Infosquito"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "iplant-email"
   {:type     :service
    :path     "services/iplant-email"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "JEX"
   {:type     :service
    :path     "services/JEX"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "kifshare"
   {:type     :service
    :path     "services/kifshare"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "metadactyl-clj"
   {:type     :service
    :path     "services/metadactyl-clj"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "NotificationAgent"
   {:type     :service
    :path     "services/NotificationAgent"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "OSM"
   {:type     :service
    :path     "services/OSM"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "Panopticon"
   {:type     :tool
    :path     "services/Panopticon"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "clavin"
   {:type     :tool
    :path     "tools/clavin"
    :build    :cmdtar
    :rpm?     true
    :tarball? true
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "facepalm"
   {:type     :tool
    :path     "tools/facepalm"
    :build    :cmdtar
    :rpm?     true
    :tarball? true
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "filetool"
   {:type     :tool
    :path     "tools/filetool"
    :build    :lein
    :rpm?     true
    :tarball? false
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "proboscis"
   {:type     :tool
    :path     "tools/proboscis"
    :build    :cmdtar
    :rpm?     false
    :tarball? true
    :uberjar? true
    :install? false
    :language :clojure
    :archive? true}

   "de-database-schema"
   {:type     :database
    :path     "databases/de-database-schema"
    :build    :shell
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? false
    :language :sql
    :archive? true}

   "notification-db"
   {:type     :database
    :path     "databases/notification-db"
    :build    :shell
    :rpm?     false
    :tarball? false
    :uberjar? false
    :install? false
    :language :sql
    :archive? true}})


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
(def rpm-projects     (filter-vals :rpm? projects))


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


(defn project-deps
  "Returns a vector containing a projects dependencies"
  [path-to-project-file]
  (:dependencies (apply hash-map (drop 3 (read-string (slurp path-to-project-file))))))


(defn iplant-deps
  "Returns the iplant dependencies from a list of dependencies"
  [dep-list]
  (filter #(.startsWith (str (first %1)) "org.iplantc") dep-list))


(defn- extract-project-name
  "Given a dependency tuple, returns the name of the project."
  [tuple]
  (let [prj-name (name (first tuple))]
    (if (= prj-name "clojure-commons")
      "iplant-clojure-commons"
      prj-name)))


(defn project-dirs-from-dep
  "Returns the project directory associated with the provided dep"
  [dep-list]
  (map #(:path (get projects (extract-project-name %))) dep-list))


(def checkout-dir "checkouts")

(defn- find-folders
  "Returns the folder the given filename resides in, ignoring any
   results from the checkouts directories."
  [filename]
  (map #(str (.getParentFile %1))
       (filter #(not (.contains (str %1) "checkouts/"))
               (filter #(.endsWith (str %1) filename)
                       (file-seq (io/file "."))))))


(defn delete-checkout-dirs
  "Deletes the checkouts directory from the given directory paths."
  [paths]
  (doseq [pdir paths]
    (fs/with-cwd pdir
      (when (and (fs/exists? checkout-dir)
                 (fs/directory? checkout-dir))
        (fs/delete-dir checkout-dir)))))


(defn create-checkout-dirs
  "Creates the checkouts directory in the given directory paths."
  [paths]
  (doseq [pdir paths]
    (fs/with-cwd pdir
      (when-not (fs/exists? checkout-dir)
        (fs/mkdir checkout-dir)))))


(defn create-checkout-symlinks
  "Creates the checkouts directory for the given project and
   populates it with the appropriate links."
  [project-map]
  (let [project-path (:path project-map)]
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
                              (print-shell-result (sh/sh "ln" "-sf" target-path link-path)))))))))


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
      (println ">> Installing " path-to-project)
      (print-shell-result (sh/sh "lein" "clean"))
      (print-shell-result (sh/sh "lein" "install")))))


(defmethod install :mvn
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Installing " path-to-project)
      (print-shell-result (sh/sh "mvn" "clean"))
      (print-shell-result (sh/sh "mvn" "install")))))


(defmethod install :default
  [project-map]
  (println ">> Don't know how to install " (:path project-map)))


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
      (println ">> Building " path-to-project)
      (print-shell-result (sh/sh "lein" "clean"))
      (print-shell-result (sh/sh "lein" "uberjar")))))


(defmethod build :cmdtar
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Building iplant-cmdtar project " path-to-project)
      (print-shell-result (sh/sh "lein" "clean"))
      (print-shell-result (sh/sh "lein" "uberjar"))
      (print-shell-result (sh/sh "lein" "iplant-cmdtar")))))


(defmethod build :mvn
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Building Java project " path-to-project)
      (print-shell-result (sh/sh "mvn" "clean"))
      (print-shell-result (sh/sh "mvn" "build")))))


(defmethod build :shell
  [project-map]
  (let [path-to-project (:path project-map)]
    (sh/with-sh-dir path-to-project
      (println ">> Building shell project " path-to-project)
      (print-shell-result (sh/sh "./build.sh")))))


(defmethod build :default
  [project-map]
  (println ">> Don't know how to build " (:path project-map)))


;;; RPM building is a special case.
(defn build-rpm
  "Builds a RPM for the given project. Obeys the --rpm option.
   uses the value passed in with --build-number. Uses the
   lein-iplant-rpm project under the hood."
  [opts project-map]
  (if (:rpm? project-map)
    (let [path-to-project (:path project-map)]
      (sh/with-sh-dir
       path-to-project
       (println ">> Generating RPM for " path-to-project)
       (print-shell-result (sh/sh "lein" "iplant-rpm" (:build-number opts)))))))


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
    (println ">> Copying builds from " target-path " to builds directory.")
    (print-shell-result (bash-cmd (str "mv " target-path "/*.jar " "builds")))))


(defn move-rpm-build
  "Moves all .rpm files from the project's root directory. Like all
   of the (move-*) functions, it'll fail if no .rpms exist."
  [project-map]
  (let [path-to-project (:path project-map)]
    (println ">> Copying any RPMs from " path-to-project " to builds directory.")
    (print-shell-result (bash-cmd (str "mv " path-to-project "/*.rpm " "builds")))))


(defn move-cmdtar-build
  "Moves all .tar.gz files found in the project's target directory."
  [project-map]
  (let [path-to-project (:path project-map)
        target-path     (path-join path-to-project "target")]
    (println ">> Copying any cmdtars from " path-to-project " to builds directory.")
    (print-shell-result (bash-cmd (str "mv " target-path "/*.tar.gz " "builds")))))


(defn move-database-build
  "Moves all .tar.gz files found in the project's root directory."
  [project-map]
  (let [path-to-project (:path project-map)]
    (println ">> Copying builds from " path-to-project " to builds directory.")
    (print-shell-result (sh/sh "bash" "-c" (str "mv " path-to-project "/*.tar.gz " "builds")))))


(defn- archive-project
  [opts project-map]
  (when (:archive? project-map)
    (if (:rpm opts)
      (when (:rpm? project-map)
        (move-rpm-build project-map)))

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
  (println "> Moving builds to builds directory.")
  (when (fs/exists? "builds")
    (println ">> Cleaning out builds directory")
    (print-shell-result (sh/sh "rm" "-r" "builds")))
  (println ">> Creating builds directory"))


(defn archive-builds
  "Called at the end of a execution that builds everything. It will
   archive all of the projects as appropriate."
  [opts]
  (prep-builds-dir)
  (print-shell-result (sh/sh "mkdir" "builds"))
  (archive-lein-plugins opts)
  (archive-libs opts)
  (archive-services opts)
  (archive-tools opts))


(defn do-symlinks
  [opts]
  (println "> Creating the checkouts symlinks for Clojure projects...")
  (doseq [[proj-name proj] clojure-projects]
    (println ">> Handling " (:path proj))
    (create-checkout-symlinks proj)
    (println "")))


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
  (when (:rpm opts)
    (doseq [[svc-name svc-map] services]
      (build-rpm opts svc-map)))
  (if (:archive opts)
    (archive-services opts)))


(defn do-tools
  [opts]
  (build-tools)
  (when (:rpm opts)
    (doseq [[tool-name tool-map] tools]
      (build-rpm opts tool-map)))
  (if (:archive opts)
    (archive-tools opts)))


(defn do-databases
  [opts]
  (build-databases)
  (if (:archive opts)
    (archive-databases opts)))


(defn do-everything
  [opts]
  (do-symlinks opts)
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
   ["-r" "--rpm" "Build RPM?" :default false :flag true]
   ["-b" "--build-number" "Assigns a build number" :default nil]
   ["-a" "--archive" "Archive builds?" :default false :flag true]))


(defn validate-opts
  [opts]
  (when (and (:rpm opts) (nil? (:build-number opts)))
    (println "You must specify --rpm and --build-number together")
    (System/exit 1)))

(def valid-cmds
  #{"help"
    "symlinks"
    "lein-plugins"
    "libs"
    "services"
    "tools"
    "databases"})

(defn validate-cmds
  [cmds]
  (let [invalid-cmds (filter #(not (contains? valid-cmds %)) cmds)]
    (when (pos? (count invalid-cmds))
      (println "The following are invalid commands: ")
      (doseq [cmd invalid-cmds]
        (println "* " cmd))
      (System/exit 1))))

(defn main-func
  []
  (let [args *command-line-args*
        [opts cmds help-str] (parse-args args)
        cmds        (drop 1 cmds)
        _           (validate-opts opts)
        _           (validate-cmds cmds)]
    (when (pos? (count cmds))
      (doseq [cmd cmds]
        (case cmd
          "help"         (do (println help-str)
                           (println "Commands are:" (string/join " "(seq valid-cmds)))
                           (System/exit 0))
          "symlinks"     (do-symlinks opts)
          "lein-plugins" (do-lein-plugins opts)
          "libs"         (do-libs opts)
          "services"     (do-services opts)
          "tools"        (do-tools opts)
          "databases"    (do-databases opts)
          (println "Not a valid command:" cmd))))
    (when-not (pos? (count cmds))
      (do-everything opts))))


(main-func)
