(ns heuristomancer.core
  (:gen-class)
  (:use [clojure.java.io :only [reader input-stream]]
        [clojure.tools.cli :only [cli]]
        [heuristomancer.loader :only [load-parsers]])
  (:require [instaparse.core :as insta]
            [clojure.string :as string])
  (:import [java.util.zip GZIPInputStream]
           [java.io ByteArrayInputStream]))

(def ^:private default-sample-size
  "The default sample size to use."
  1000)

(defmulti bytes->string
  "Converts a byte array to a string depending on the type of parser."
  (fn [converter sample] converter))

(defmethod bytes->string :gzip
  [converter bytes]
  (try
    (let [bis   (ByteArrayInputStream. bytes)
          gis   (GZIPInputStream. bis)
          limit (count bytes)
          buf   (byte-array limit)
          len   (.read gis buf 0 limit)]
      (if (= len -1)
        ""
        (string/join " " (map str buf))))
    (catch Exception e "")))

(defmethod bytes->string nil
  [converter bytes]
  (String. bytes))

(defn sip
  "Reads in 'limit' number of bytes from input-stream. Returns a byte-array."
  [input-stream limit]
  (with-open [r input-stream]
    (let [buf (byte-array limit)
          len (.read r buf 0 limit)]
      (if (= len -1)  ; EOF
        ""
        (byte-array (take len buf))))))

(defn format-matches
  "Determines whether a format matches a sample from a file."
  [sample [_ identifier-fn converter]]
  (let [sample-str    (bytes->string converter sample)]
    (not (insta/failure? (identifier-fn sample-str)))))

(def formats (load-parsers))

(defn identify-sample
  "Attempts to identify the type of a sample."
  [sample]
  (ffirst (filter (partial format-matches sample) formats)))

(defn identify
  "Attempts to identify the type of a sample obtained from anything that clojure.java.io/reader
   can convert to a reader."
  ([in]
     (identify in default-sample-size))
  ([in sample-size]
     (identify-sample (sip in sample-size))))

(defn parse-args
  "Parses the command-line arguments."
  [args]
  (cli args
       ["-l" "--list" "List recognized file types." :default false :flag true]
       ["-s" "--sample-size" "Sepcify the size of the sample." :parse-fn #(Integer. %)
        :default 1000]
       ["-h" "-?" "--help" "Show help." :default false :flag true]))

(defn list-formats
  "Lists all of the formats currently recognized by this utility."
  []
  (dorun (map (comp println name first) formats)))

(defn supported-formats
  "Returns a list of the formats currently recognized by heuristomancer. Returns the list
   in the order of evaluation when iterating over the parsers when attempting to id a file."
  []
  (mapv (comp name first) formats))

(defn show-file-type
  "Shows the type of a single file or 'UNRECOGNIZED' if the file type can't be identified."
  [sample-size path]
  (let [type (identify (input-stream path) sample-size)]
    (if (nil? type)
      (println path "- UNRECOGNIZED")
      (println path "-" (name type)))))

(defn show-file-types
  "Shows the types of a sequence of files."
  [sample-size paths]
  (dorun (map (partial show-file-type sample-size) paths)))

(defn -main
  [& args]
  (let [[opts args banner] (parse-args args)]
    (cond
     (:help opts) (println banner)
     (:list opts) (list-formats)
     :else        (show-file-types (:sample-size opts) args))))
