(ns anon-files.serve
  (:use [ring.util.response]
        [ring.util.time])
  (:require [clj-jargon.init :as init]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.item-info :as info]
            [clj-jargon.permissions :as perms]
            [clj-jargon.paging :as paging]
            [taoensso.timbre :as timbre]
            [common-cfg.cfg :as cfg]
            [clojure.string :as string]))

(timbre/refer-timbre)

(defn jargon-cfg
  []
  (dosync
   (init/init
    (:irods-host @cfg/cfg)
    (:irods-port @cfg/cfg)
    (:irods-user @cfg/cfg)
    (:irods-password @cfg/cfg)
    (:irods-home @cfg/cfg)
    (:irods-zone @cfg/cfg)
    "")))

(defn range-request?
  [req]
  (and (contains? req :headers)
       (contains? (:headers req) "range")))

(defn contains-bytes-string?
  "Returns true if the header starts with 'bytes'."
  [header]
  (let [trimmed-header (string/trim header)]
    (.startsWith trimmed-header "bytes")))

(defn trim-equals
  "Returns a string with the leading equals sign trimmed off. The
   entire header is trimmed of leading and trailing whitespace as a result."
  [header]
  (let [trimmed-header (string/trim header)]
    (if (.startsWith trimmed-header "=")
      (string/replace-first trimmed-header "=" "")
      trimmed-header)))

(defn trim-bytes
  "Returns a string with the leading 'bytes=' trimmed off. The entire header is trimmed
   of leading and trailing whitespace as a result."
  [header]
  (let [trimmed-header (string/trim header)]
    (if (.startsWith trimmed-header "bytes")
      (string/replace-first trimmed-header "bytes" "")
      trimmed-header)))

(defn multiple-ranges?
  "Returns true if the header field specifies multiple ranges."
  [header]
  (not= (.indexOf header ",") -1))

(defn extract-byte-ranges
  "Returns a vector of tuples."
  [header]
  (re-seq #"[0-9]*\s*-\s*[0-9]*" header))

(defn categorize-ranges
  "Categorize ranges based on whether they're a bounded range, an unbounded
   range, or a single byte request. The return value will be in the format:
       {
           :range \"rangestring\"
           :kind \"kind string\"
       }
   Values for :kind can be 'bounded', 'unbounded', or 'byte'."
  [ranges]
  (let [mapify     (fn [range kind] {:range range :kind kind})
        bounded?   (fn [range] (re-seq #"[0-9]+\s*-\s*[0-9]+" range))
        unbounded? (fn [range] (re-seq #"[0-9]+\s*-\s*" range))
        end-byte?  (fn [range] (re-seq #"\s*-\s*[0-9]+" range))
        range-kind (fn [range]
                     (cond
                      (bounded? range)   "bounded"
                      (unbounded? range) "unbounded"
                      (end-byte? range)  "unbounded-negative"
                      :else              "unknown"))]
    (map #(mapify %1 (range-kind %1)) ranges)))

(defn parse-ranges
  "Parses ranges based on type. A range of type will have an :lower and :upper field added,
   a range of type unbounded will have a :lower field and no :upper field. A field of :byte
   will have a :lower and :upper bound that is set to the same value. An unknown range will
   not have any fields added.

   The input should be a seq of maps returned by (categorize-ranges)."
  [ranges]
  (let [upper          (fn [range] (last (re-seq #"[0-9]+" (:range range))))
        lower          (fn [range] (first (re-seq #"[0-9]+" (:range range))))
        extract-byte   (fn [range] (first (re-seq #"\s*-\s*[0-9]+" (:range range))))          
        bounded-type   (fn [range] (assoc range :upper (upper range) :lower (lower range)))
        unbounded-type (fn [range] (assoc range :lower (lower range)))
        unbounded-neg  (fn [range] (assoc range :lower (extract-byte range)))
        byte-type      (fn [range] (assoc range :upper (extract-byte range) :lower (extract-byte range)))
        delegate       (fn [range]
                         (case (:kind range)
                           "bounded"
                           (bounded-type range)

                           "unbounded"
                           (unbounded-type range)

                           "unbounded-negative"
                           (unbounded-neg range)

                           "byte"
                           (byte-type range)
                           range))]
    (map delegate ranges)))

(defn extract-ranges
  "Parses the range header and returns a list of ranges. The returned value will be the
   same as (parse-ranges)."
  [req]
  (-> (get-in req [:headers "range"])
      (trim-bytes)
      (trim-equals)
      (extract-byte-ranges)
      (categorize-ranges)
      (parse-ranges)))

(defmacro validated
  [cm filepath & body]
  `(cond
     (not (info/exists? ~cm ~filepath))
     (do (warn "[anon-files]" ~filepath "does not exist.")
       (not-found "Not found."))

     (not (info/is-file? ~cm ~filepath))
     (do (warn "[anon-files]" ~filepath "is not a file.")
       (-> (response "Not a file.") (status 403)))

     (not (perms/is-readable? ~cm (:anon-user @cfg/cfg) ~filepath))
     (do (warn "[anon-files]" ~filepath "is not readable.")
       (-> (response "Not allowed.") (status 403)))

     :else
     ~@body))

(defn- base-file-header
  [cm filepath]
  {"Accept-Ranges"    "bytes"
   "Cache-Control"    "no-cache"
   "ETag"             (str "W/" (info/lastmod-date cm filepath))
   "Expires"          "0"
   "Vary"             "*"
   "Content-Location" filepath})

(defn- file-header
  ([cm filepath start-byte end-byte]
     (merge (base-file-header cm filepath)
            {"Content-Length" (str (- end-byte start-byte))}))
  ([cm filepath]
     (merge (base-file-header cm filepath)
            {"Content-Length" (str (info/file-size cm filepath))})))

(defn serve
  [cm filepath]
  (validated cm filepath (ops/input-stream cm filepath)))

(defn- range-body
  [cm filepath start-byte end-byte]
  (if (pos? (- end-byte start-byte))
    (java.io.ByteArrayInputStream.
     (paging/read-at-position cm filepath start-byte (- end-byte start-byte) false))))

(defn range-response
  [cm filepath start-byte end-byte]
  {:status   206
   :body    (range-body cm filepath start-byte end-byte)
   :headers (file-header cm filepath start-byte end-byte)})

(defn not-satisfiable-response
  [cm filepath]
  {:status 416
   :body "The requested range is not satisfiable."
   :headers {"Accept-Ranges" "bytes"
             "Content-Range" (str "bytes */" (info/file-size cm filepath))}})

(defn calc-lower
  [lower-val]
  (if-not (pos? lower-val)
    0
    lower-val))

(defn calc-upper
  [upper-val file-size]
  (if (> upper-val file-size)
    file-size
    upper-val))

(defn handle-bounded-request
  [cm filepath range]
  (let [file-size (info/file-size cm filepath)
        lower     (calc-lower (Integer/parseInt (:lower range)))
        upper     (calc-upper (Integer/parseInt (:upper range)) file-size)]
    (cond
     (> lower upper)
     (not-satisfiable-response cm filepath)

     (> lower file-size)
     (not-satisfiable-response cm filepath)

     (= lower upper)
     (range-response cm filepath lower (inc lower))

     :else
     (range-response cm filepath lower upper))))

(defn handle-unbounded-request
  [cm filepath range]
  (let [file-size (info/file-size cm filepath)
        lower     (calc-lower (Integer/parseInt (:lower range)))
        upper     file-size]
    (cond
     (> lower upper)
     (not-satisfiable-response cm filepath)

     (> lower file-size)
     (not-satisfiable-response cm filepath)

     (= lower upper)
     (range-response cm filepath lower (inc lower))

     :else
     (range-response cm filepath lower upper))))

(defn handle-unbounded-negative-request
  [cm filepath range]
  (let [file-size (info/file-size cm filepath)
        lower     (calc-lower (+ file-size (- (Integer/parseInt (:lower range)) 1)))
        upper     (calc-upper (- file-size 1) file-size)]
    (cond
     (> lower upper)
     (not-satisfiable-response cm filepath)

     (> lower file-size)
     (not-satisfiable-response cm filepath)

     (= lower upper)
     (range-response cm filepath lower (inc lower))

     :else
     (range-response cm filepath lower upper))))

(defn handle-byte-request
  [cm filepath range]
  (let [file-size (info/file-size cm filepath)
        lower     (calc-lower (Integer/parseInt (:lower range)))
        upper     (calc-upper (+ (Integer/parseInt (:lower range)) 1) file-size)]
    (cond
     (> lower upper)
     (not-satisfiable-response cm filepath)
     
     (> lower file-size)
     (not-satisfiable-response cm filepath)
     
     (= lower upper)
     (range-response cm filepath lower (inc lower))
     
     :else
     (range-response cm filepath lower upper))))

(defn serve-range
  [cm filepath range]
  (validated
   cm filepath
   (case (:kind range)
     "bounded"
     (handle-bounded-request cm filepath range)

     "unbounded"
     (handle-unbounded-request cm filepath range)

     "unbounded-negative"
     (handle-unbounded-negative-request cm filepath range)

     "byte"
     (handle-byte-request cm filepath range)

     (serve cm filepath))))

(defn handle-request
  [req]
  (info "Handling GET request for" (:uri req))
  (info "\n" (cfg/pprint-to-string req))
  (init/with-jargon (jargon-cfg) [cm]
    (try
      (if (range-request? req)
        (let [ranges   (extract-ranges req)
              filepath (:uri req)]
          (if-not ranges
            (not-satisfiable-response cm filepath)
            (serve-range cm filepath (first ranges))))
        (serve (:uri req)))
      (catch Exception e
        (warn e)))))

(defn handle-head-request
  [req]
  (info "Handling head request for" (:uri req))
  (info "\n" (cfg/pprint-to-string req))
  (init/with-jargon (jargon-cfg) [cm]
    (validated cm (:uri req)
      {:status 200
       :body ""
       :headers (file-header cm (:uri req))})))
