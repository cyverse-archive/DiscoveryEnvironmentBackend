(ns clj-jargon.gen-query
  (:require [clojure.string :as string])
  (:import [org.irods.jargon.core.query IRODSGenQuery]
           [org.irods.jargon.core.query RodsGenQueryEnum]))

(defn result-row->vec
  [rr]
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

(defn column-xformer
  [col]
  (cond
   (= (type col) RodsGenQueryEnum)
   (.getName col)

   :else
   (string/replace col #"['\\\\]" escape-gen-query-char)))

(defn gen-query-col-names
  [cols]
  (into-array (mapv column-xformer cols)))

(defn execute-gen-query
  [cm sql cols]
  (.getResults
   (.executeIRODSQueryAndCloseResult
    (:executor cm)
    (IRODSGenQuery/instance
     (String/format sql (gen-query-col-names cols))
     50000)
    0)))
