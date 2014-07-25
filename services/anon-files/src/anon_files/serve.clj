(ns anon-files.serve
  (:use [ring.util.response])
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

(def range-regex #"\s*(bytes)\s*=\s*([0-9]+)\s*\-\s*([0-9]+)\s*$")
(def unbound-range-regex #"\s*(bytes)\s*=\s*([0-9]+)\s*\-\s*$")

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
                      (end-byte? range)  "byte"
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
        byte-type      (fn [range] (assoc range :upper (extract-byte range) :lower (extract-byte range)))
        delegate       (fn [range]
                         (case (:kind range)
                           "bounded"   (bounded-type range)
                           "unbounded" (unbounded-type range)
                           "byte"      (byte-type range)
                           range))]
    (map delegate ranges)))

(defn valid-range?
  [req]
  (let [range-header (get-in req [:headers "range"])]
    (or (re-seq range-regex range-header)
        (re-seq unbound-range-regex range-header))))

(defn bound-range?
  [range-header]
  (re-seq range-regex range-header))

(defn unbound-range?
  [range-header]
  (re-seq unbound-range-regex range-header))

(defn- extract-bound-range
  [range-header]
  (let [range-matches (re-matches range-regex range-header)]
    [(Long/parseLong (nth range-matches 2)) (Long/parseLong (nth range-matches 3))]))

(defn- extract-unbound-range
  [range-header]
  (let [range-matches (re-matches unbound-range-regex range-header)]
    [(Long/parseLong (nth range-matches 2))]))

(defn extract-range
  [req]
  (let [range-header (get-in req [:headers "range"])]
    (cond
     (bound-range? range-header)
     (do (debug "detected bounded range")
       (extract-bound-range range-header))

     (unbound-range? range-header)
     (do (debug "detected unbounded range")
       (extract-unbound-range range-header)))))

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

(defn- file-header
  ([cm filepath start-byte end-byte]
   {"Accept-Ranges" "bytes"
    "Content-Length" (str (- end-byte start-byte))})
  ([cm filepath]
   {"Accept-Ranges" "bytes"
    "Content-Length" (str (info/file-size cm filepath))}))

(defn serve
  [filepath]
  (init/with-jargon (jargon-cfg) [cm]
    (validated cm filepath
      (ops/input-stream cm filepath))))

(defn- range-body
  [cm filepath start-byte end-byte]
  (if (pos? (- end-byte start-byte))
    (java.io.ByteArrayInputStream.
     (paging/read-at-position cm filepath start-byte (- end-byte start-byte) false))))

(defn range-response
  [cm filepath start-byte end-byte]
  {:status   200
   :body    (range-body cm filepath start-byte end-byte)
   :headers (file-header cm filepath start-byte end-byte)})

(defn serve-range
  [filepath [start-byte end-byte]]
  (init/with-jargon (jargon-cfg) [cm]
    (validated cm filepath
      (if (nil? end-byte)
       (range-response cm filepath start-byte (info/file-size cm filepath))
       (range-response cm filepath start-byte end-byte)))))

(defn handle-request
  [req]
  (info "Handling GET request for" (:uri req))
  (info "\n" (cfg/pprint-to-string req))
  (try
   (cond
    (and (range-request? req) (valid-range? req))
    (do (debug "extracted range:" (extract-range req))
      (serve-range (:uri req) (extract-range req)))

    (and (range-request? req) (not (valid-range? req)))
    (-> (response "Invalid range request.") (status 500))

    :else
    (serve (:uri req)))
    (catch Exception e
      (warn e))))

(defn handle-head-request
  [req]
  (info "Handling head request for" (:uri req))
  (info "\n" (cfg/pprint-to-string req))
  (init/with-jargon (jargon-cfg) [cm]
    (validated cm (:uri req)
      {:status 200
       :body ""
       :headers (file-header cm (:uri req))})))
