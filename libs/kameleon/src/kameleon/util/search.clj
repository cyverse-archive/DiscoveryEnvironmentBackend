(ns kameleon.util.search
  (:use [kameleon.core]
        [kameleon.entities]
        [korma.core])
  (:require [clojure.string :as str]))

(defn format-query-wildcards
  [search-term]
  (str/replace search-term #"[%_*?]" {"%" "\\%",
                                      "_" "\\_",
                                      "*" "%",
                                      "?" "_"}))
