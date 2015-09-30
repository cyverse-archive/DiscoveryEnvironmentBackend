(ns anon-files.serve
  (:use [ring.util.response]
        [ring.util.time])
  (:require [clj-jargon.init :as init]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.item-info :as info]
            [clj-jargon.permissions :as perms]
            [clj-jargon.paging :as paging]
            [clojure.tools.logging :as log]
            [common-cfg.cfg :as cfg]
            [clojure.string :as string]
            [anon-files.inputs :as inputs]))

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
  [^String header]
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

(defn valid?
  [cm filepath]
  (cond
   (not (info/exists? cm filepath))
   (do (log/warn "[anon-files]" filepath "does not exist.")
       false)

   (not (info/is-file? cm filepath))
   (do (log/warn "[anon-files]" filepath "is not a file.")
       false)

   (not (perms/is-readable? cm (:anon-user @cfg/cfg) filepath))
   (do (log/warn "[anon-files]" filepath "is not readable.")
       false)

   :else true))

(defmacro validated
  [cm filepath & body]
  `(cond
     (not (info/exists? ~cm ~filepath))
     (do (log/warn "[anon-files]" ~filepath "does not exist.")
       (not-found "Not found."))

     (not (info/is-file? ~cm ~filepath))
     (do (log/warn "[anon-files]" ~filepath "is not a file.")
       (-> (response "Not a file.") (status 403)))

     (not (perms/is-readable? ~cm (:anon-user @cfg/cfg) ~filepath))
     (do (log/warn "[anon-files]" ~filepath "is not readable.")
       (-> (response "Not allowed.") (status 403)))

     :else
     ~@body))

(defn- base-file-header
  [filepath lastmod]
  {"Accept-Ranges"    "bytes"
   "Cache-Control"    "no-cache"
   "ETag"             (str "W/" lastmod)
   "Expires"          "0"
   "Vary"             "*"
   "Content-Location" filepath})

(defn- file-header
  ([filepath lastmod start-byte end-byte]
     (merge (base-file-header filepath lastmod)
            {"Content-Length" (str (inc (- end-byte start-byte)))})))

(defn serve
  [cm filepath]
  (validated cm filepath (ops/input-stream cm filepath)))

(defn- range-body
  [cm filepath start-byte end-byte]
  (if (>= (- end-byte start-byte) 0)
    (inputs/chunk-stream cm filepath start-byte end-byte)))

(defn not-satisfiable-response
  [filesize]
  {:status  416
   :body    "The requested range is not satisfiable."
   :headers {"Accept-Ranges" "bytes"
             "Content-Range" (str "bytes */" filesize)}})

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

(defn- handle-range-request
  [cm filepath file-size lower upper num-bytes]
  (log/warn
     "File information:\n"
     "File Path:" filepath "\n"
     "File Size:" file-size "\n"
     "Lower Bound:" lower "\n"
     "Upper Bound:" upper "\n"
     "Number bytes:" num-bytes "\n")
  (cond
   (> lower upper)
   (not-satisfiable-response file-size)

   (> lower file-size)
   (not-satisfiable-response file-size)

   (= lower upper)
   (range-body cm filepath lower upper)

   :else
   (range-body cm filepath lower upper)))

(defn bounded-request-info
  [cm filepath range]
  (let [file-size (info/file-size cm filepath)
        retval {:file-size file-size
                :lastmod   (info/lastmod-date cm filepath)
                :lower     (calc-lower (Long/parseLong (:lower range)))
                :upper     (calc-upper (Long/parseLong (:upper range)) file-size)
                :kind      (:kind range)}]
    (assoc retval :num-bytes (inc (- (:upper retval) (:lower retval))))))

(defn unbounded-request-info
  [cm filepath range]
  (let [file-size (info/file-size cm filepath)
        retval {:file-size file-size
                :lastmod   (info/lastmod-date cm filepath)
                :lower     (calc-lower (Long/parseLong (:lower range)))
                :upper     (dec file-size)
                :kind      (:kind range)}]
    (assoc retval :num-bytes (inc (- (:upper retval) (:lower retval))))))

(defn unbounded-negative-info
  [cm filepath range]
  (let [file-size (info/file-size cm filepath)
        retval {:file-size file-size
                :lastmod   (info/lastmod-date cm filepath)
                :lower     (calc-lower (+ file-size (- (Long/parseLong (:lower range)) 1)))
                :upper     (calc-upper (- file-size 1) file-size)
                :kind      (:kind range)}]
    (assoc retval :num-bytes (inc (- (:upper retval) (:lower retval))))))

(defn byte-request-info
  [cm filepath range]
  (let [file-size (info/file-size cm filepath)
        retval {:file-size file-size
                :lastmod   (info/lastmod-date cm filepath)
                :lower     (calc-lower (Long/parseLong (:lower range)))
                :upper     (calc-upper (+ (Long/parseLong (:lower range)) 1) file-size)
                :kind      (:kind range)}]
    (assoc retval :num-bytes (inc (- (:upper retval) (:lower retval))))))

(defn normal-request-info
  [cm filepath range]
  (let [filesize (info/file-size cm filepath)]
    {:file-size filesize
     :lastmod   (info/lastmod-date cm filepath)
     :lower     0
     :upper     (dec filesize)
     :num-bytes filesize
     :kind      (:kind range)}))

(defn make-request
  [cm filepath info]
  (let [file-size (:file-size info)
        lower     (:lower info)
        upper     (:upper info)
        num-bytes (:num-bytes info)]
    (handle-range-request cm filepath file-size lower upper num-bytes)))

(defn request-info
  [cm filepath range]
  (case (:kind range)
    "bounded"
    (bounded-request-info cm filepath range)

    "unbounded"
    (unbounded-request-info cm filepath range)

    "unbounded-negative"
    (unbounded-negative-info cm filepath range)

    "byte"
    (byte-request-info cm filepath range)

    (normal-request-info cm filepath range)))

(defn content-range-str
  [info]
  (let [kind  (:kind info)
        lower (:lower info)
        upper (:upper info)
        filesize (:file-size info)]
    (case kind
      "bounded"
      (str "bytes " lower "-" upper "/" filesize)

      "unbounded"
      (str "bytes " lower "-" "/" filesize)

      "unbounded-negative"
      (str "bytes " lower "-" "/" filesize)

      "byte"
      (str "bytes " lower "-" upper "/" filesize)

      (str "bytes " lower "-" upper "/" filesize))))

(defn log-headers
  [response]
  (log/warn "Response map:\n" (dissoc response :body))
  response)

(defn anon-input-stream
  [filepath filesize info]
  (init/with-jargon (jargon-cfg) [cm]
    (if-not range
      (not-satisfiable-response filesize)
      (make-request cm filepath info))))

(defn get-req-info
  [req]
  (init/with-jargon (jargon-cfg) [cm]
    (if-not (valid? cm (:uri req))
      (throw (Exception. "Bad")))
    (let [range (first (extract-ranges req))]
      (request-info cm (:uri req) range))))

(defn handle-request
  [req]
  (log/info "Handling GET request for" (:uri req))
  (log/info "\n" (cfg/pprint-to-string req))
  (try
    (if (range-request? req)
      (log-headers
       (let [info     (get-req-info req)
             body     (anon-input-stream (:uri req) (:filesize info) info)]
         (if (map? body)
           body
           {:status  206
            :body    body
            :headers (assoc (file-header (:uri req) (:lastmod info) (:lower info) (:upper info))
                       "Content-Range" (content-range-str info))})))
      (init/with-jargon (jargon-cfg) [cm]
        (serve cm (:uri req))))
    (catch Exception e
      (log/warn e))))

(defn handle-head-request
  [req]
  (log/info "Handling head request for" (:uri req))
  (log/info "\n" (cfg/pprint-to-string req))
  (init/with-jargon (jargon-cfg) [cm]
    (log-headers
     (validated cm (:uri req)
                (if (range-request? req)
                  (let [info (get-req-info req)]
                    {:status  200
                     :body    ""
                     :headers (assoc (file-header (:uri req) (:lastmod info) (:lower info) (:upper info))
                                "Content-Range"  (content-range-str info))})
                  (let [lastmod  (info/lastmod-date cm (:uri req))
                        filesize (info/file-size cm (:uri req))]
                    {:status 200
                     :body ""
                     :headers (file-header (:uri req) lastmod 0 (dec filesize))}))))))

(defn- build-options-response
  [cm {:keys [uri]}]
  (let [lastmod  (info/lastmod-date cm uri)
        filesize (info/file-size cm uri)]
    {:status  200
     :body    ""
     :headers (assoc (base-file-header uri lastmod)
                "Allow" "GET, HEAD")}))

(defn handle-options-request
  [req]
  (log/info "Handling options request for" (:uri req))
  (log/info "\n" (cfg/pprint-to-string req))
  (init/with-jargon (jargon-cfg) [cm]
    (log-headers (validated cm (:uri req) (build-options-response cm req)))))
