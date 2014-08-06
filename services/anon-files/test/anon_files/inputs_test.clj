(ns anon-files.inputs-test
  (:require [clojure.test :refer :all]
            [anon-files.inputs :refer :all]
            [clojure.java.io :refer :all]))

(deftest test-get-chunk-size
  (is (= (get-chunk-size 8192) 4096))
  (is (= (get-chunk-size 4095) 4095))
  (is (= (get-chunk-size 9) 9)))


(deftest test-get-num-chunks
  (is (= (get-num-chunks default-chunk-size 8192) 2))
  (is (= (get-num-chunks default-chunk-size 8193) 2)))

(deftest test-get-remainder-bytes
  (is (= (get-remainder-bytes default-chunk-size 4096) 0))
  (is (= (get-remainder-bytes default-chunk-size 8192) 0))
  (is (= (get-remainder-bytes default-chunk-size 4097) 1))
  (is (= (get-remainder-bytes default-chunk-size 8193) 1)))

(def test-array (byte-array 10))
(def test-array1 (byte-array 10))
(def test-array2 (byte-array 10))
(def test-array3 (byte-array 4098))
(def test-dropped (byte-array 5)) ; drop 5 bytes
(def test-dropped1 (byte-array 3)) ; drop 7 bytes
(def test-dropped2 (byte-array 9)) ; drop 1 byte
(def test-dropped3 (byte-array 2)) ; drop 4096 bytes
(java.util.Arrays/fill test-array (byte 122))
(java.util.Arrays/fill test-array1 (byte 122))
(java.util.Arrays/fill test-array2 (byte 122))
(java.util.Arrays/fill test-array3 (byte 122))
(java.util.Arrays/fill test-dropped (byte 122))
(java.util.Arrays/fill test-dropped1 (byte 122))
(java.util.Arrays/fill test-dropped2 (byte 122))
(java.util.Arrays/fill test-dropped3 (byte 122))

(deftest test-drop-bytes
  (is (= (slurp (drop-bytes (input-stream test-array) 5))
         (slurp (input-stream test-dropped))))
  (is (= (slurp (drop-bytes (input-stream test-array1) 7))
         (slurp (input-stream test-dropped1))))
  (is (= (slurp (drop-bytes (input-stream test-array2) 1))
         (slurp (input-stream test-dropped2))))
  (is (= (slurp (drop-bytes (input-stream test-array3) 4096))
         (slurp (input-stream test-dropped3)))))

(def test-chunk (byte-array 4098))
(java.util.Arrays/fill test-chunk (byte 122))

(deftest test-chunk-stream
  (is (= (slurp (chunk-stream (input-stream test-chunk) 10 20))
         "zzzzzzzzzzz")))
