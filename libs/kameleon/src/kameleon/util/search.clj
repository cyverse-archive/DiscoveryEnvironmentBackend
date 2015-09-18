(ns kameleon.util.search
  (:require [clojure.string :as str]))

(defn format-query-wildcards
  [search-term]
  (str/replace search-term #"[%_*?]" {"%" "\\%",
                                      "_" "\\_",
                                      "*" "%",
                                      "?" "_"}))
