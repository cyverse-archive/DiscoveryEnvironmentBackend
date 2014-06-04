(ns clojure-commons.file-utils
  (:use [clojure.java.io :only [file]]
        [clojure.string :only [join split]])
  (:require [me.raynes.fs :as fs])
  (:import [java.io File]))

(def ^:dynamic *max-temp-dir-attempts*
  "The maximum number of times to attempt to create a temporary directory."
  10)

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
      (str (apply file paths')))))

(defn rm-last-slash
  "Returns a new version of 'path' with the last slash removed.

   Parameters:
     path - String containing a path.

   Returns: New version of 'path' with the trailing slash removed."
  [path]
  (.replaceAll path "/$" ""))

(defn basename
  "Returns the basename of 'path'.

   This works by calling getName() on a java.io.File instance. It's prefered
   over last-dir-in-path for that reason.

   Parameters:
     path - String containing the path for an item in iRODS.

   Returns:
     String containing the basename of path."
  [path]
  (.getName (file path)))

(defn dirname
  "Returns the dirname of 'path'.

   This works by calling getParent() on a java.io.File instance.

   Parameters:
     path - String containing the path for an item in iRODS.

   Returns:
     String containing the dirname of path."
  [path]
  (.getParent (file path)))

(defn add-trailing-slash
  "Adds a trailing slash to 'input-string' if it doesn't already have one."
  [input-string]
  (if-not (.endsWith input-string "/")
    (str input-string "/")
    input-string))

(defn normalize-path
  "Normalizes a file path on Unix systems by eliminating '.' and '..' from it.
   No attempts are made to resolve symbolic links."
  [file-path]
  (loop [dest [] src (split file-path #"/")]
    (if (empty? src)
      (join "/" dest)
      (let [curr (first src)]
        (cond (= curr ".") (recur dest (rest src))
              (= curr "..") (recur (vec (butlast dest)) (rest src))
              :else (recur (conj dest curr) (rest src)))))))

(defn abs-path
  "Converts a path to an absolute path."
  [file-path]
  (normalize-path (.getAbsolutePath (file file-path))))

(defn abs-path?
  "Returns true if the path passed in is an absolute path."
  [file-path]
  (.isAbsolute (file file-path)))

(defn file?
  "Tests whether the path is a file."
  [file-path]
  (.isFile (file file-path)))

(defn dir?
  "Tests whether the path is a directory."
  [file-path]
  (.isDirectory (file file-path)))

(defn exists?
  "Tests whether the given paths exist on the filesystem."
  [& filepaths]
  (every? #(.exists %) (map file filepaths)))

(defn rec-delete
  "Recursively deletes all files in a directory structure rooted at the given
   directory.  Note that this recursion does consume stack space.  This
   shouldn't be a problem, however, because a directory structure that is deep
   enough to cause a stack overflow will probably create a path that is too
   long for the OS to support."
  [f]
  (when (.isDirectory f)
    (dorun (map rec-delete (.listFiles f))))
  (.delete f))

(defn- mk-temp-dir
  "Attempts to create a temporary directory named by the provided function."
  [name-fn]
  (loop [idx 0]
    (if-not (>= idx *max-temp-dir-attempts*)
      (let [f (name-fn idx)]
        (if (.mkdir f) f (recur (inc idx))))
      nil)))

(defn temp-dir
  "Creates a temporary directory.  This function is used by the with-temp-dir
   macro to create the temporary directory."
  ([prefix err-fn]
     (temp-dir prefix (file (System/getProperty "user.dir")) err-fn))
  ([prefix parent err-fn]
     (let [base          (str prefix (System/currentTimeMillis) "-")
           temp-dir-file (fn [idx] (file parent (str base idx)))
           temp-dir      (mk-temp-dir temp-dir-file)]
       (when (nil? temp-dir)
         (err-fn (.getPath parent) prefix base))
       temp-dir)))

(defmacro with-temp-dir
  "Creates a temporary directory and binds a file representing the path to the
   temporary directory within the user's current working directory.  The body
   is executed in a try expression with a finally clause that recursively
   deletes the directory.  If the directory can't be created then the provided
   error function will be called with three arguments: the path to the parent
   directory, the directory name prefix and the base name that was used.  The
   dynamic variable, *max-temp-dir-attempts*, can be used to specify the
   maximum number of times to try to create the temporary directory.  The
   default value of this variable is 10."
  [sym prefix err-fn & body]
  `(let [~sym (temp-dir ~prefix ~err-fn)]
     (try
       (.delete ~sym)
       (.mkdir ~sym)
       (fs/with-cwd ~sym
         ~@body)
       (finally (rec-delete ~sym)))))

(defmacro with-temp-dir-in
  "Creates a temporary directory inside a specified parent directory and binds a
   file representing the path to the temporary directory to a provided symbol.
   The body is executed in a try expression with a finally clause that
   recursively deletes the directory.  If the directory can't be created then
   the provided error function will be called with three arguments: the path to
   the parent directory, the directory name prefix and the base name that was
   used.  The dynamic variable, *max-temp-dir-attempts*, can be used to specify
   the maximum number of times to try to create the temporary directory  The
   default value of this variable is 10."
  [sym parent prefix err-fn & body]
  `(let [~sym (temp-dir ~prefix ~parent ~err-fn)]
     (try
       (.delete ~sym)
       (.mkdir ~sym)
       (fs/with-cwd ~sym
         ~@body)
       (finally (rec-delete ~sym)))))
