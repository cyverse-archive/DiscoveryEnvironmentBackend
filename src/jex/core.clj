(ns jex.core
  (:gen-class)
  (:use [jex.config]
        [compojure.core]
        [ring.middleware
         params
         keyword-params
         nested-params
         multipart-params
         cookies
         session]
        [clojure-commons.error-codes]
        [clojure.java.classpath]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :as rsp-utils]
            [ring.adapter.jetty :as jetty]
            [jex.process :as jp]
            [jex.json-body :as jb]
            [clojure.java.io :as ds]
            [clojure.tools.logging :as log]
            [cheshire.core :as cheshire]))

(defn do-submission
  "Handles a request on /. "
  [request]
  (try
    (let [body (:body request)]
      (log/warn "Received job request:")
      (log/warn (cheshire/encode body))
      
      (if (jp/validate-submission body)
        (let [[exit-code dag-id doc-id] (jp/submit body)]
          (cond
            (not= exit-code 0)
            (throw+ {:error_code "ERR_FAILED_NON_ZERO"})
            
            :else
            {:sub_id dag-id
             :osm_id doc-id}))
        (throw+ {:error_code "ERR_INVALID_JSON"})))
    (catch Exception e
      (throw+ {:error_code "ERR_UNHANDLED_EXCEPTION"}))))

(defroutes jex-routes
  (GET "/" [] "Welcome to the JEX.")

  (POST "/" request
        (trap "submit" do-submission request))

  (POST "/arg-preview" request
        (trap "arg-preview" jp/cmdline-preview (:body request)))

  (DELETE "/stop/:uuid" [uuid]
          (trap "stop" jp/stop-analysis uuid)))

(defn site-handler [routes]
  (-> routes
    jb/parse-json-body
    wrap-errors))

(defn -main
  [& args]
  (load-config-from-zookeeper)
  (jetty/run-jetty (site-handler jex-routes) {:port (listen-port)}))
