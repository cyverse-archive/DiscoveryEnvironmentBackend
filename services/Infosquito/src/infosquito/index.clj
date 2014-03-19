(ns infosquito.index
  (:require [clojure-commons.file-utils :as file]))


(defn indexable?
  [index-base collection]
  (let [home       (file/path-join index-base "home")
        trash      (file/path-join index-base "trash")
        home-trash (file/path-join trash "home")]
    (and (not= index-base collection)
         (not= home collection)
         (not= trash collection)
         (not= home-trash collection)
         (not= home (file/dirname collection))
         (not= home-trash (file/dirname collection)))))