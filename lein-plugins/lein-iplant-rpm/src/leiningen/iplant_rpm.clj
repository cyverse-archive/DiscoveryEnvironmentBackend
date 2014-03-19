(ns leiningen.iplant-rpm
  (:use [clojure.java.io :only [file copy reader]]
        [clojure.pprint :only [pprint]]
        [fleet]
        [leiningen.core.classpath :only [add-repo-auth get-proxy-settings]]
        [leiningen.core.main :only [abort *exit-process?*]]
        [leiningen.core.eval :only [sh]])
  (:require [leiningen.iplant.aether :as aether]
            [clojure.string :as string])
  (:import [java.io FilenameFilter]
           [java.net UnknownHostException]))

;; Templates to use for various project types.
(def ^{:private true} templates-by-type
  {:service {:spec "rpm/svc-spec.fleet"
             :exec "rpm/svc-init.fleet"}
   :command {:spec "rpm/cmd-spec.fleet"
             :exec "rpm/cmd-exec.fleet"}
   :jetty   {:spec "rpm/jetty-spec.fleet"
             :exec "rpm/jetty-init.fleet"}})

;; The path to various RPM directories.
(def ^{:private true} rpm-base-dir (file "/usr/src/redhat"))
(def ^{:private true} rpm-spec-dir (file rpm-base-dir "SPECS"))
(def ^{:private true} rpm-source-dir (file rpm-base-dir "SOURCES"))
(def ^{:private true} rpm-build-dir (file rpm-base-dir "BUILD"))
(def ^{:private true} rpm-dir (file rpm-base-dir "RPMS/noarch"))

(defn- inform
  "Prints an informational message to standard output."
  [& ms]
  (println (string/join " " ms))
  (flush))

(defn- warn
  "Prints a warning message to standard error output."
  [& ms]
  (binding [*out* *err*]
    (println (string/join " " ms))
    (flush)))

(defn- template-for
  "Returns the template for the given project type and template type."
  [project-type template-type]
  (get-in templates-by-type [project-type template-type]))

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

(defn- to-relative
  "Converts canonical absolute paths that refer to subdirectories of the
   current working directory to relative paths."
  [paths]
  (let [file-sep (System/getProperty "file.separator")
        working-dir (str (System/getProperty "user.dir") file-sep)]
    (map #(string/replace % (re-pattern (str "^\\Q" working-dir))  "") paths)))

(defn- update-policies
  "Fills in the update policies in a repository definition."
  [update checksum [repo-name opts]]
  [repo-name (assoc opts
               :update   (or update :daily)
               :checksum (or checksum :fail))])

(defn- repository-defs
  "Extract the repository definitions from a project."
  [{:keys [repositories update checksum]}]
  (->> repositories
       (map add-repo-auth)
       (map (partial update-policies update checksum))))

(defn- dependency-files
  "Resolves only the direct dependencies for a project."
  [project]
  (try
    (aether/resolve-direct-dependencies
     :local-repo   (:local-repo project)
     :offline?     (:offline? project)
     :repositories (repository-defs project)
     :coordinates  (:dependencies project)
     :mirrors      (:mirrors project)
     :proxy        (get-proxy-settings))
    (catch UnknownHostException e
      (if-not (:offline? project)
        (dependency-files (assoc project :offline? true))
        (throw e)))))

(defn- find-file
  "Finds a file name matching a regular expression in a sequence of file names."
  [re msg names]
  (let [f (first (filter (partial re-matches re) names))]
    (when-not f
      (throw (Exception. msg)))
    f))

(def ^:private find-jetty-runner
  (partial find-file #"jetty-runner-.*[.]jar"
           "A jetty-runner dependency is required for :jetty projects"))

(def ^:private find-war-file
  (partial find-file #".*[.]war"
           "A WAR file dependency is required for :jetty projects"))

(def ^:private clavin-dep
  "iplant-clavin >= 1.2.0-29")

(defn- build-rpm-dependencies
  "Builds the RPM dependencies to use for the RPM.  Jetty projects have a
   special dependency on Clavin, which has to be added to the list if it's not
   already there."
  [{:keys [dependencies type]
    :or   {dependencies []}}]
  (let [depends-on-clavin? #(some (partial re-find #"\Aiplant-clavin") %)]
   (cond
    (not= type :jetty)                dependencies
    (depends-on-clavin? dependencies) dependencies
    :else                             (cons clavin-dep dependencies))))

(defn- project-to-settings
  "Converts a project map to the settings map that we need to fill in the
   templates."
  [project release]
  (let [settings  (:iplant-rpm project {})
        type      (:type settings :service)
        jetty?    (= type :jetty)
        dep-files (when jetty? (dependency-files project))
        dep-names (when jetty? (map #(.getName %) dep-files))
        main      (:main project)]
    (assoc settings
           :summary        (:summary settings "")
           :name           (:name project)
           :version        (first (string/split (:version project) #"-"))
           :release        release
           :provides       (:provides settings (:name project))
           :type           type
           :include-deps   (= type :jetty)
           :dependencies   (build-rpm-dependencies settings)
           :description    (:description project "")
           :jar-version    (:version project)
           :config-files   (:config-files settings [])
           :config-path    (:config-path settings)
           :exe-files      (:exe-files settings [])
           :runuser        (:runuser settings "iplant")
           :resource-paths (to-relative (:resource-paths project []))
           :main           (when main (string/replace main "-" "_"))
           :repositories   (:repositories project)
           :lein-dep-files dep-files
           :lein-deps      dep-names
           :jetty-runner   (when jetty? (find-jetty-runner dep-names))
           :war-file       (when jetty? (find-war-file dep-names))
           :zk-service     (:zk-service settings (:name project)))))

(defn- validate-settings
  "Verifies that this plugin can process the project settings."
  [settings]
  (when (nil? (get templates-by-type (:type settings)))
    (throw (Exception. (str "unknown project type: " (:type settings)))))
  (when (and (seq? (:config-files settings)) (nil? (:config-path settings)))
    (throw (Exception. (str "config-path is required in projects with "
                            "configuration files")))))

(defn- build-and-validate-settings
  "Builds and validates the settings map for this plugin."
  [project release]
  (doto (project-to-settings project release)
    validate-settings))

(defn- gen-file
  "Generates a file with the given name using the given template name."
  [settings file-name template-name]
  (spit file-name (str ((load-template template-name) settings))))

(defn- mkdirs
  "Creates a directory and any parent directories that need to be created.  If
   the directory already exists then this function is a no-op."
  [dir]
  (let [f (file dir)]
    (if (.exists f)
      (when-not (.isDirectory f)
        (throw (Exception. (str dir " exists and is not a directory"))))
      (when-not (.mkdirs f)
        (throw (Exception. (str "unable to create " dir)))))))

(declare rec-copy)

(defn- copy-dir
  "Copies the contents of a directory to another directory."
  [dest f]
  (mkdirs dest)
  (rec-copy dest (seq (.listFiles f))))

(defn- copy-file-or-dir
  "Copies either a file or a directory."
  [dir f]
  (when (.exists f)
    (let [dest (file dir (.getName f))]
      (cond (.isFile f)      (copy f dest)
            (.isDirectory f) (copy-dir dest f)
            :else (throw     (Exception. "unrecognized file type"))))))

(defn- rec-copy
  "Performs a recursive copy of one or more files.  Note that recursion does
   consume stack space.  This shouldn't be a problem, however, because a
   directory structure that is deep enough to cause a stack overflow will
   probably create a path that is too long for the OS to support."
  [dir fs]
  (dorun (map #(copy-file-or-dir dir %) fs)))

(defn- copy-dir-structure
  "Copies files or directories to a destination directory, preserving the
   relative paths of the source files or directories."
  [dir fs]
  (dorun (map #(let [dest-dir (.getParentFile (file dir %))]
                 (mkdirs (.getPath dest-dir))
                 (rec-copy dest-dir [(file %)]))
              (remove nil? fs))))

(defn- rec-delete
  "Recursively deletes all files in a directory structure rooted at the given
   directory.  Note that this recursion does consume stack space.  This
   shouldn't be a problem, however, because a directory structure that is deep
   enough to cause a stack overflow will probably create a path that is too
   long for the OS to support."
  [f]
  (when (.isDirectory f)
    (dorun (map rec-delete (.listFiles f))))
  (.delete f))

(defn- build-spec-file
  "Builds the RPM specification file."
  [settings]
  (let [spec-name (str (:name settings) ".spec")]
    (gen-file settings spec-name (template-for (:type settings) :spec))
    spec-name))

(defn- make-build-dir
  "Creates the build directory, which will be used to generate the source
   tarball."
  [build-dir settings exec-name]
  (let [config-dir     (file (:config-path settings))
        resource-paths (:resource-paths settings)
        exe-files      (:exe-files settings)
        type           (:type settings)
        lein-dep-files (:lein-dep-files settings)]
    (mkdirs build-dir)
    (rec-copy build-dir (map file [exec-name "project.clj" "src"]))
    (copy-dir-structure build-dir (conj resource-paths config-dir))
    (copy-dir-structure build-dir exe-files)
    (when (= type :jetty)
      (rec-copy build-dir lein-dep-files))))

(defn- exec
  "Executes a command, throwing an exception if the command fails."
  [& args]
  (let [status (apply sh args)]
    (when (not= status 0)
      (let [cmd (string/join " " args)]
        (throw (Exception. (str cmd " failed with status " status)))))))

(defn- build-source-tarball
  "Builds the source tarball that will be used by rpmbuild to generate the
   RPM and returns the base name of the generated tarball, which is needed
   for cleanup work."
  [settings]
  (let [build-dir    (file (str (:provides settings) "-" (:version settings)))
        tarball-name (str build-dir ".tar.gz")
        exec-name    (:name settings)]
    (inform "Building the source tarball...")
    (gen-file settings exec-name (template-for (:type settings) :exec))
    (make-build-dir build-dir settings exec-name)
    (exec "tar" "czvf" tarball-name (.getPath build-dir))
    (rec-delete build-dir)
    [build-dir tarball-name]))

(defn- delete-existing-files
  "Deletes existing files in the given directory with the given extension."
  [dir ext]
  (let [filt (proxy [FilenameFilter] []
               (accept [dir filename]
                       (.endsWith filename ext)))]
    (dorun (map #(.delete %) (.listFiles dir filt)))))

(defn- move
  "Moves a file to a new location or file name."
  [src dest]
  (copy src dest)
  (.delete src))

(defn- clean-up-old-files
  "Cleans up any files that may be left over from previous builds."
  []
  (inform "Cleaning up files from previous builds...")
  (let [working-dir (file (System/getProperty "user.dir"))]
    (delete-existing-files working-dir ".rpm")
    (delete-existing-files working-dir ".tar.gz")))

(defn- validate-release
  "Validates the release number, which must be a positive integer."
  [release]
  (when-not (re-matches #"\d+" release)
    (throw (Exception. (str "invalid release number: " release)))))

(defn- rpm-filename
  "Determines the name of the resulting RPM file."
  [source-dir release]
  (file (str source-dir "-" release ".noarch.rpm")))

(defn- build-rpm
  "Builds the RPM."
  [prj release args]
  (validate-release release)
  (clean-up-old-files)
  (let [settings                  (build-and-validate-settings prj release)
        [source-dir tarball-name] (build-source-tarball settings)
        tarball-file              (file tarball-name)
        tarball-path              (file rpm-source-dir tarball-name)
        spec-file                 (file (build-spec-file settings))
        spec-dest                 (file rpm-spec-dir spec-file)
        rpm-file                  (file (rpm-filename source-dir release))
        working-dir (file (System/getProperty "user.dir"))]
    (when-not (args :dry-run)
      (inform "Staging files for rpmbuild...")
      (copy spec-file spec-dest)
      (move tarball-file tarball-path)
      (inform "Running rpmbuild...")
      (exec "rpmbuild" "-ba" (.getPath spec-dest))
      (inform "Getting generated RPMs and cleaning up...")
      (move (file rpm-dir rpm-file) (file working-dir rpm-file))
      (rec-delete (file rpm-build-dir source-dir)))))

(defn iplant-rpm
  "Generates the type of RPM that is used by the iPlant Collaborative to
   distribute web services written in Clojure."
  [project release & args]
  (try
    (build-rpm project release (set (map keyword args)))
    (catch Exception e
      (.printStackTrace e *err*)
      (flush)
      (binding [*exit-process?* true]
        (abort)))))
