(ns anon-files.serve
  (:use [ring.util.response]
        [anon-files.config])
  (:require [clj-jargon.init :as init]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.item-info :as info]
            [clj-jargon.permissions :as perms]
            [clj-jargon.paging :as paging]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(defn jargon-cfg
  []
  (dosync
   (init/init
    (:irods-host @props)
    (:irods-port @props)
    (:irods-user @props)
    (:irods-password @props)
    (:irods-home @props)
    (:irods-zone @props)
    "")))

(defn range-request?
  [req]
  (and (contains? req :headers)
       (contains? (:headers req) "range")))

(def range-regex #"\s*(bytes)\s*=\s*([0-9]+)\s*\-\s*([0-9]+)\s*$")
(def unbound-range-regex #"\s*(bytes)\s*=\s*([0-9]+)\s*\-\s*$")

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

     (not (perms/is-readable? ~cm (:anon-user @props) ~filepath))
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
      {:status  200
       :body    (ops/input-stream cm filepath)
       :headers (file-header cm filepath)})))

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
  (info "\n" (pprint-to-string req))
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
  (info "\n" (pprint-to-string req))
  (init/with-jargon (jargon-cfg) [cm]
    (validated cm (:uri req)
      {:status 200
       :body ""
       :headers (file-header cm (:uri req))})))
