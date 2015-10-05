(ns facepalm.conversions
  (:use [clojure.java.io :only [file reader]]
        [cemerick.pomegranate :only [add-dependencies]])
  (:require [clojure.string :as string]
            [clojure-commons.file-utils :as fu]
            [me.raynes.fs :as fs])
  (:import [java.io PushbackReader]))

(def ^:private dependency-filename
  "dependencies.clj")

(def ^:private default-repositories
  {"central" "http://repo1.maven.org/maven2"
   "clojars" "http://clojars.org/repo/"
   "iplant"  "http://katic.iplantcollaborative.org/archiva/repository/internal/"})

(defn- drop-extension
  [fname]
  (first (string/split fname #"\.")))

(defn- split-on-underscore
  [fname]
  (string/split fname #"_"))

(defn- dotize
  [vstr]
  (string/join "." (into [] vstr)))

(defn- fmt-version
  [[version-str date-str]]
  [(-> version-str
       (string/replace #"^c" "")
       dotize)
   date-str])

(defn- fmt-date-str
  [date-str]
  (let [date-vec (into [] date-str)]
    (str
     (string/join (take 8 date-vec)) "." (string/join (take-last 2 date-vec)))))

(defn- fmt-date
  [[vstr date-str]]
  [vstr (fmt-date-str date-str)])

(defn- db-version
  [parts]
  (string/join ":" parts))

(defn- fname->db-version
  [fname]
  (-> fname
      fu/basename
      drop-extension
      split-on-underscore
      fmt-version
      fmt-date
      db-version))

(defn- fname->ns-str
  [fname]
  (-> (str "facepalm." fname)
      (string/replace #"\.clj$" "")
      (string/replace #"_" "-")))

(defn- ns-str->cv-str
  [ns-str]
  (str ns-str "/convert"))

(defn- fname->cv-ref
  [fname]
  (-> fname
      fu/basename
      fname->ns-str
      ns-str->cv-str
      symbol
      eval))

(defn- list-conversions
  []
  (map str (fs/find-files "conversions" #"^c.*_[0-9]{10}\.clj$")))

(defn- load-conversions
  [cv-list]
  (doseq [cv cv-list]
    (load-file cv)))

(defn- load-dependency-file
  [f]
  (with-open [r (PushbackReader. (reader f))]
    (binding [*read-eval* false]
      (read r))))

(defn load-dependencies
  []
  (let [f (fs/file "conversions" dependency-filename)]
    (when (.isFile f)
      (let [{:keys [dependencies repositories]} (load-dependency-file f)]
        (add-dependencies :coordinates dependencies
                          :repositories (merge default-repositories repositories))))))

(defn conversion-map
  []
  (load-dependencies)
  (let [conversions (list-conversions)]
    (load-conversions conversions)
    (into {} (map #(vector (fname->db-version %) (fname->cv-ref %)) conversions))))
