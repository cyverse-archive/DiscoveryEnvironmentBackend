(ns iplant-email.exceptions
  (:require [clojure.tools.logging :as log]
            [cheshire.core :as cheshire]))

(defn format-exception
  "Formats a raised exception as a JSON object. Returns a response map."
  [exception]
  (log/debug "format-exception")
  (let [string-writer (java.io.StringWriter.)
        print-writer  (java.io.PrintWriter. string-writer)]
    (. exception printStackTrace print-writer)
    (let [localized-message (. exception getLocalizedMessage)
          stack-trace       (. string-writer toString)]
      (log/warn localized-message stack-trace)
      {:status 500
       :body (cheshire/encode {:message     localized-message
                               :stack-trace stack-trace})})))
