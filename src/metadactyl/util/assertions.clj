(ns metadactyl.util.assertions
  "Assertions for metadactyl services."
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.string :as string]
            [clojure-commons.error-codes :as ce]))

(defmacro assert-not-nil
  "Throws an exception if the result of a group of expressions is nil.

   Parameters:
     id-field - the name of the field to use when storing the ID.
     id       - the identifier to store in the ID field."
  [[id-field id] & body]
  `(let [res# (do ~@body)]
     (if (nil? res#)
       (throw+ {:error_code ce/ERR_NOT_FOUND
                ~id-field   ~id})
       res#)))

(defmacro assert-not-blank
  "Throws an exception if the result of a group of expresssions is blank.

   Parameters:
     field - the name of the field whose value is blank."
  [[field] & body]
  `(let [res# (do ~@body)]
     (if (string/blank? res#)
       (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
                :field      ~field})
       res#)))
