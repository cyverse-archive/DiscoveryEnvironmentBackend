(ns anon-files.serve
  (:use [ring.util.response]
        [anon-files.config])
  (:require [clj-jargon.init :as init]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.item-info :as info]
            [clj-jargon.permissions :as perms]
            [clj-jargon.paging :as paging]))

(defn jargon-cfg
  []
  (init/init
  (irods-host)
  (irods-port)
  (irods-user)
  (irods-pass)
  (irods-home)
  (irods-zone)
  ""))

(defn range-request?
  [req]
  (and (contains? req :headers)
       (contains? (:headers req) "range")))

(def range-regex #"\s*(bytes)\s*=\s*([0-9]+)\s*\-\s*([0-9]+)\s*")

(defn valid-range?
  [req]
  (re-seq range-regex (get-in req [:headers "range"])))

(defn extract-range
  [req]
  (let [range-header  (get-in req [:headers "range"])
        range-matches (re-matches range-regex range-header)]
    [(Long/parseLong (nth range-matches 2)) (Long/parseLong (nth range-matches 3))]))

(defn serve
  [filepath]
  (init/with-jargon (jargon-cfg) [cm]
    (cond
     (not (info/exists? cm filepath))
     (do (println "[anon-files]" filepath "does not exist.")
       (not-found "Not found."))

     (not (info/is-file? cm filepath))
     (do (println "[anon-files]" filepath "is not a file.")
       (-> (response "Not a file.") (status 403)))

     (not (perms/is-readable? cm (anon-user) filepath))
     (do (println "[anon-files]" filepath "is not readable.")
       (-> (response "Insufficient privileges.") (status 403)))

     :else
     (ops/input-stream cm filepath))))

(defn serve-range
  [filepath [start-byte end-byte]]
  (init/with-jargon (jargon-cfg) [cm]
    (cond
     (not (info/exists? cm filepath))
     (do (println "[anon-files]" filepath "does not exist.")
       (not-found "Not found."))

     (not (info/is-file? cm filepath))
     (do (println "[anon-files]" filepath "is not a file.")
       (-> (response "Not a file.") (status 403)))

     (not (perms/is-readable? cm (anon-user) filepath))
     (do (println "[anon-files]" filepath "is not readable.")
       (-> (response "Insufficient privileges.") (status 403)))

     :else
     {:status 206
      :body
      (java.io.ByteArrayInputStream.
       (paging/read-at-position cm filepath start-byte (- end-byte start-byte) false))
      :headers
      {"Content-Range"
       (str "bytes " start-byte "-" end-byte)

       "Accept-Ranges"
       "bytes"}})))

(defn handle-request
  [req]
  (cond
   (and (range-request? req) (valid-range? req))
   (serve-range (:uri req) (extract-range req))

   (and (range-request? req) (not (valid-range? req)))
   (-> (response "Invalid range request.") (status 500))

   :else
   (serve (:uri req))))
