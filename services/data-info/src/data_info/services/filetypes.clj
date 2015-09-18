(ns data-info.services.filetypes
  (:require [heuristomancer.core :as hm]
            [data-info.util.logging :as dul]
            [dire.core :refer [with-pre-hook! with-post-hook!]]))

(def script-types (sort (hm/supported-formats)))

(defn get-type-list
  []
  {:types script-types})

(defn do-type-list
  []
  (get-type-list))

(with-pre-hook! #'do-type-list
  (fn []
    (dul/log-call "do-type-list")))

(with-post-hook! #'do-type-list (dul/log-func "do-type-list"))
