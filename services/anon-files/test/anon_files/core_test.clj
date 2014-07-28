(ns anon-files.core-test
  (require [clojure.test :refer :all]
           [anon-files.serve :refer :all]))

(deftest test-contains-bytes-string
  (is (contains-bytes-string? "bytes boo"))
  (is (not (contains-bytes-string? "boo"))))

(deftest test-trim-equals
  (is (= "boo" (trim-equals "=boo")))
  (is (= " boo" (trim-equals " = boo "))))

(deftest test-multiple-ranges
  (is (multiple-ranges? "0-10,11-20"))
  (is (not (multiple-ranges? "0-10"))))

(deftest test-extract-byte-ranges
  (is (= (extract-byte-ranges "0-10,11-20,21-30")
         '("0-10" "11-20" "21-30")))
  (is (= (extract-byte-ranges "-10,11-")
         '("-10" "11-"))))

(deftest test-categorize-ranges
  (is (= (categorize-ranges '("0-10" "11-20" "21-30"))
         '({:range "0-10" :kind "bounded"}
           {:range "11-20" :kind "bounded"}
           {:range "21-30" :kind "bounded"})))
  (is (= (categorize-ranges '("-10" "11-"))
         '({:range "-10" :kind "unbounded-negative"}
           {:range "11-" :kind "unbounded"})))
  (is (= (categorize-ranges '("11" "12"))
         '({:range "11" :kind "unknown"}
           {:range "12" :kind "unknown"})))
  (is (= (categorize-ranges '("0-10" "12" "13-" "-1"))
         '({:range "0-10" :kind "bounded"}
           {:range "12" :kind "unknown"}
           {:range "13-" :kind "unbounded"}
           {:range "-1" :kind "unbounded-negative"}))))

(deftest test-parse-ranges
  (is (= (parse-ranges '({:range "0-10" :kind "bounded"}
                         {:range "11-20" :kind "bounded"}
                         {:range "21-30" :kind "bounded"}))
         '({:range "0-10" :kind "bounded" :lower "0" :upper "10"}
           {:range "11-20" :kind "bounded" :lower "11" :upper "20"}
           {:range "21-30" :kind "bounded" :lower "21" :upper "30"})))
  (is (= (parse-ranges '({:range "-10" :kind "unbounded-negative"}
                         {:range "11-" :kind "unbounded"}))
         '({:range "-10" :kind "unbounded-negative" :lower "-10"}
           {:range "11-" :kind "unbounded" :lower "11"})))
  (is (= (parse-ranges '({:range "11" :kind "unknown"}
                         {:range "12" :kind "unknown"}))
         '({:range "11" :kind "unknown"}
           {:range "12" :kind "unknown"})))
  (is (= (parse-ranges '({:range "0-10" :kind "bounded"}
                         {:range "12" :kind "unknown"}
                         {:range "13-" :kind "unbounded"}
                         {:range "-1" :kind "unbounded-negative"}))
         '({:range "0-10" :kind "bounded" :lower "0" :upper "10"}
           {:range "12" :kind "unknown"}
           {:range "13-" :kind "unbounded" :lower "13"}
           {:range "-1" :kind "unbounded-negative" :lower "-1"}))))

(deftest test-extract-headers
  (is (= (extract-ranges {:headers {"range" "byte=0-20"}})
         '({:range "0-20" :lower "0" :upper "20" :kind "bounded"})))
  (is (= (extract-ranges {:headers {"range" "byte=20-"}})
         '({:range "20-" :lower "20" :kind "unbounded"})))
  (is (= (extract-ranges {:headers {"range" "byte=-10"}})
         '({:range "-10" :kind "unbounded-negative" :lower "-10"}))))
