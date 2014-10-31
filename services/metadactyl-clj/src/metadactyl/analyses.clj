(ns metadactyl.analyses
  (:use [metadactyl.util.conversions :only [remove-nil-vals]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [metadactyl.analyses.base :as ab]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.util.config :as config]))

(defn submit-job
  [submission]
  (try+
   (http/post (config/jex-base-url)
              {:body         (cheshire/encode submission)
               :content-type :json})
   (catch Object o
     (log/error (:throwable &throw-context) "job submission failed")
     (throw+ {:error_code ce/ERR_REQUEST_FAILED})))
  submission)

(defn submit
  [{:keys [user email]} submission]
  (->> (ab/build-submission user email submission)
       (remove-nil-vals)
       (submit-job)))
