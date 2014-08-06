(ns anon-files.inputs
  (:use [clj-jargon.init]
        [clj-jargon.item-ops]
        [clj-jargon.item-info])
  (:import [java.io InputStream]))

(def default-chunk-size 4096)

(defn get-chunk-size
  [file-size]
  (loop [fsize file-size
         csize default-chunk-size]
    (if (< fsize csize)
      fsize
      csize)))

(defn get-num-chunks
  [chunk-size file-size]
  (int (Math/floor (/ file-size chunk-size))))

(defn get-remainder-bytes
  [chunk-size file-size]
  (mod file-size chunk-size))

(defn drop-bytes
  [istream num-bytes]
  (let [csize      (get-chunk-size num-bytes)
        num-chunks (get-num-chunks csize num-bytes)
        remainder  (get-remainder-bytes csize num-bytes)]
    (let [buf (byte-array csize)]
      (doseq [iter (range num-chunks)]
        (.read istream buf 0 csize)))
    (let [buf (byte-array remainder)]
      (.read istream buf 0 remainder))
    istream))

(defn chunk-stream
  [istream start-byte end-byte]
  (drop-bytes istream start-byte)
  (let [location (atom start-byte)]
    (proxy [java.io.InputStream] []
      (available [] (.available istream))
      (mark [readlimit] (.mark istream readlimit))
      (markSupported [] (.markSupported istream))
      (read
        ([]
           (let [new-loc (inc @location)]
             (if (< new-loc end-byte)
               (let [bufsize 1
                     buf     (byte-array bufsize)]
                 (.read istream buf 0 bufsize)
                 (reset! location new-loc)
                 (first buf))
               -1)))
        ([b]
           (if (< @location end-byte)
             (let [diff       (inc (- end-byte @location))
                   len        (if (> (count b) diff)
                                diff
                                (count b))
                   bytes-read (.read istream b 0 len)]
               (reset! location (+ @location bytes-read))
               bytes-read)
             -1))
        ([b off len]
           (if (< @location end-byte)
             (let [diff (inc (- end-byte @location))
                   len (if (> len diff)
                         diff
                         len)
                   bytes-read (.read istream b off len)]
               (reset! location (+ @location bytes-read))
               bytes-read)
             -1)))
      (reset [] (.reset istream))
      (skip [n] (.skip istream n))
      (close []
        (.close istream)))))
