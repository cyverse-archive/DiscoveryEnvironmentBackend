(ns facepalm.error-codes
  (:use [fleet]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import [java.util Locale]))

(def ^:private default-locale-dirname "en-US")

(def locale-dirnames
  "Determines the directory names to use for the default locale in order of
   precedence."
  (memoize
   (fn []
     (let [locale  (Locale/getDefault)
           dirname (str (.getLanguage locale) "-" (.getCountry locale))]
       (map #(str "error-templates/" %)
            (if (= dirname default-locale-dirname)
              [dirname]
              [dirname default-locale-dirname]))))))

(defn- get-resource
  ([filename dirnames]
     (first (filter (comp not nil?)
                    (map #(get-resource (str % "/" filename)) dirnames))))
  ([filename]
     (let [loader (.. (Thread/currentThread) getContextClassLoader)]
       (.getResourceAsStream loader filename))))

(defn- load-template
  "Attempts to load a template for an error message in each of the locale
   directory names."
  [template-name]
  (if-let [in (get-resource (str template-name ".fleet") (locale-dirnames))]
    (with-open [in in]
      (fleet [data] (slurp in) {:escaping :bypass}))))

(defn options-missing
  "Throws an exception indicating that required options are missing.  If
   multiple option names are given then it's assumed that only one of the
   options must be specified."
  [& opt-names]
  (let [opts (string/join ", " (map #(str "--" (name %)) opt-names))]
    (throw+ {:type      ::required-options-missing
            :opt-names opts})))

(defn build-artifact-retrieval-failed
  "Throws an exception indicating that the build artifact could not be
   retrieved from the specified Jenkins job or QA drop."
  [status artifact-url]
  (throw+ {:type   ::build-artifact-retrieval-failed
           :status status
           :url    artifact-url}))

(defn database-tarball-copy-failed
  "Throws an exception indicating that the database tarball couldn't be copied
   from the specified location."
  [src dst reason]
  (throw+ {:type   ::database-tarball-copy-failed
           :src    src
           :dst    dst
           :reason reason}))

(defn command-execution-failed
  "Throws an exception indicating that a command could not be executed.  This
   usually means that a required program isn't installed or isn't on the user's
   path."
  [cmd reason]
  (throw+ {:type   ::command-execution-failed
           :cmd    (string/join " " cmd)
           :reason reason}))

(defn build-artifact-expansion-failed
  "Throws an exception indicating that the database build artifact could not be
   expanded.  Usually, this means that the specified build artifact is not a
   tarball."
  []
  (throw+ {:type ::build-artifact-expansion-failed}))

(defn temp-directory-creation-failure
  "Throws an exception indicating that the creation of a temporary directory
   failed.  Usually, this means that the user doesn't have write permission
   in the current working directory."
  [parent prefix base]
  (throw+ {:type   ::temp-directory-creation-failure
           :parent parent
           :prefix prefix
           :base   base}))

(defn database-connection-failure
  "Throws an exception indicating that the database connection attempt failed.
   Usually, this means that the database credentials or connection settings
   are wrong."
  [host port database user]
  (throw+ {:type ::database-connection-failure
           :host     host
           :port     port
           :database database
           :user     user}))

(defn unknown-mode
  "Throws an exception indicating that the user specified an unknown mode of
   operation on the command line."
  [mode]
  (throw+ {:type ::unknown-mode
           :mode mode}))

(defn conversion-validation-error
  "Throws an exception indicating that the validation for a conversion step
   failed."
  [db-version m]
  (throw+ (merge m {:type       ::conversion-validation-error
                    :db-version db-version})))

(defn no-password-supplied
  "Throws an exception indicating that a password was not found in .pgpass and
   no password was supplied at the password prompt."
  [host port database user]
  (throw+ {:type     ::no-password-supplied
           :host     host
           :port     port
           :database database
           :user     user}))

(defn incompatible-database-conversion
  "Throws an exception indicating that a conversion is being done for a database
   version that is not compatible with the current build of facepalm."
  [compatible-version requested-version]
  (throw+ {:type               ::incompatible-database-version
           :requested-version  requested-version
           :compatible-version compatible-version}))

(defn error-exit
  "Prints a message to standard error output and exits with a non-zero exit
   status."
  [& strs]
  (binding [*out* *err*]
    (println strs)
    (System/exit 1)))

(defn handle-slingshot-exception
  "Handles a Slingshot exception."
  [m banner]
  (let [error-fn (load-template (name (:type m)))]
    (if (nil? error-fn)
      (log/error "Unexpected exception:" m)
      (binding [*out* *err*]
        (println (str (error-fn (assoc m :banner banner)))))))
  (System/exit 1))

(defn handle-java-exception
  "Handles a java exception."
  [e]
  (log/error e "Unexpected exception:")
  (System/exit 1))

(defmacro trap
  "Traps exceptions and prints readable error messages for all expected
   exceptions.  Stack traces will still be printed for unexpected
   exceptions."
  [banner & body]
  `(try+
    ~@body
    (catch map? m# (handle-slingshot-exception m# ~banner))
    (catch Exception e# (handle-java-exception e#))))
