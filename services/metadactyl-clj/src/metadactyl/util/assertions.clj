(ns metadactyl.util.assertions
  "Assertions for metadactyl services."
  (:use [slingshot.slingshot :only [throw+]]))

(defmacro assert-not-nil
  "Throws an exception if the result of a group of expressions is nil.

   Parameters:
     id-field - the name of the field to use when storing the ID.
     id       - the identifier to store in the ID field."
  [[id-field id] & body]
  `(let [res# (do ~@body)]
     (if (nil? res#)
       (throw+ {:type     :clojure-commons.exception/not-found
                :error    (str "The item with the following ID could not be found: " ~id)
                ~id-field (str ~id)})
       res#)))
