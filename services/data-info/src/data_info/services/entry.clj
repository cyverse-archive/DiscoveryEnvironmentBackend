(ns data-info.services.entry
  "This namespace provides the business logic for all entries endpoints."
  (:require [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clojure-commons.validators :as cv]
            [data-info.util.logging :as log]
            [data-info.util.validators :as duv]
            [data-info.services.exists :as exists]
            [data-info.services.updown :as updown]))


(defn id-exists?
  [params]
  (exists/exists? params))

(with-pre-hook! #'id-exists?
  (fn [params]
    (log/log-call "exists?" params)
    (duv/valid-uuid-param "entry" (:entry params))
    (cv/validate-map params {:user string?})))

(with-post-hook! #'id-exists? (log/log-func "exists?"))


(defn get-by-path
  [path-in-zone params]
  (updown/dispatch-entries-path path-in-zone params))
