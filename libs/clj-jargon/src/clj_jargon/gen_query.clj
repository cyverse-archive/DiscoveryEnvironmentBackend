(ns clj-jargon.gen-query
  (:require [clojure.string :as string])
  (:import [org.irods.jargon.core.query IRODSGenQuery]
           [org.irods.jargon.core.query RodsGenQueryEnum]
           [org.irods.jargon.core.query IRODSQueryResultRow]
           [org.irods.jargon.core.pub IRODSGenQueryExecutor]))

(defn result-row->vec
  [^IRODSQueryResultRow rr]
  (vec (.getColumnsAsList rr)))

(defmacro print-result
  [form]
  `(let [res# ~form]
     (println res#)
     res#))

(defn- escape-gen-query-char
  [c]
  (cond (= c "\\") "\\\\\\\\"
        :else      (str "\\\\" c)))

(defmulti column-xformer type)
(defmethod column-xformer RodsGenQueryEnum
  [^RodsGenQueryEnum col]
  (.getName col))
(defmethod column-xformer :default
   [col]
   (string/replace col #"['\\\\]" escape-gen-query-char))

(defn gen-query-col-names
  [cols]
  (into-array (mapv column-xformer cols)))

(defn execute-gen-query
  [{^IRODSGenQueryExecutor executor :executor} sql cols]
  (.getResults
   (.executeIRODSQueryAndCloseResult
    executor
    (IRODSGenQuery/instance
     (String/format sql (gen-query-col-names cols))
     50000)
    0)))
