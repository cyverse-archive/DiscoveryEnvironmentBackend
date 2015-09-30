(ns anon-files.inputs
  (:use [clj-jargon.init]
        [clj-jargon.item-ops]
        [clj-jargon.item-info]
        [clj-jargon.paging])
  (:import [java.io InputStream]
           [org.irods.jargon.core.pub.io IRODSFileFactory]))

(defn chunk-stream
  [{^IRODSFileFactory file-factory :fileFactory} ^String filepath start-byte end-byte]
  (let [raf (.instanceIRODSRandomAccessFile file-factory filepath)
        location (atom start-byte)]
    (.seek raf start-byte SEEK-CURRENT)
    (proxy [java.io.InputStream] []
      (available [] (.length raf))
      (mark [readlimit] nil)
      (markSupported [] nil)
      (read
        ([]
           (let [new-loc (inc @location)]
             (if (<= new-loc end-byte)
               (let [bufsize 1
                     buf     (byte-array bufsize)]
                 (.read raf buf 0 bufsize)
                 (reset! location new-loc)
                 (first buf))
               -1)))
        ([b]
           (if (<= @location end-byte)
             (let [diff       (inc (- end-byte @location))
                   len        (if (> (count b) diff)
                                diff
                                (count b))
                   bytes-read (.read raf b 0 len)]
               (reset! location (+ @location bytes-read))
               bytes-read)
             -1))
        ([b off len]
           (if (<= @location end-byte)
             (let [diff (inc (- end-byte @location))
                   len (if (> len diff)
                         diff
                         len)
                   bytes-read (.read raf b off len)]
               (reset! location (+ @location bytes-read))
               bytes-read)
             -1)))
      (reset [] (.seek raf 0 SEEK-START))
      (skip [n] (.skipBytes raf n))
      (close []
        (.close raf)))))
