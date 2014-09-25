(ns donkey.services.coge
  (:use [donkey.auth.user-attributes]
        [donkey.util.service :only [decode-json prepare-forwarded-request]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [donkey.clients.data-info :as data]
            [donkey.util.config :as config]))

(defn- build-coge-request
  "Builds a request object with the given paths for the COGE genome viewer service."
  [paths ticket]
  (let [body {:restricted true ;; URL in response can be made public by setting this to false.
              :items (map #(hash-map :type "irods" :path %) paths)
              :ticket ticket}]
    (prepare-forwarded-request
      {:content-type "application/json; charset=utf-8"}
      (cheshire/encode body))))

(defn- get-error-details
  "Retrieves the details from an error returned by the COGE service."
  [error]
  (cond (nil? error)    "NO_ERROR_DETAILS"
        (string? error) error
        :else           (slurp (:body error))))

(defn- coge-genome-service-error
  "Throws an exception indicating that the COGE genome service encountered an error."
  [error]
  (let [details (get-error-details error)
        body {:action  "coge_genome_viewer"
              :message "unable to parse COGE genome viewer data"
              :details details
              :success false}]
    (log/error "the COGE genome service encountered an error:" details)
    (throw+ {:type :error-status
             :res  {:status       (if (or (string? error)
                                          (not (:status error)))
                                    500
                                    (:status error))
                    :content-type :json
                    :body         (cheshire/generate-string body)}})))

(defn- share-paths
  "Shares the given paths with the COGE user so the genome viewer service can access them."
  [paths]
  (let [sharer      (:shortUsername current-user)
        share-withs [(config/coge-user)]
        perms       :write]
    (data/share sharer share-withs paths perms)))


(defn- request-coge-genome-url
  "Sends a request for a genome viewer URL to the COGE service."
  [paths]
  (let [ticket   (get-proxy-ticket (config/coge-genome-load-url))
        request  (build-coge-request paths ticket)
        response (client/post (config/coge-genome-load-url) request)]
    (when-not (< 199 (:status response) 300)
      (coge-genome-service-error response))
    (string/trim (slurp (:body response)))))

(defn- parse-genome-viewer-response
  "Parses a response from the COGE genome viewer for errors or the genome's URL."
  [response]
  (let [response (decode-json response)]
    (when-not (:success response)
      (coge-genome-service-error (:error response)))
    {:coge_genome_url (:link response)}))

(defn get-genome-viewer-url
  "Retrieves a genome viewer URL by sharing the given paths and sending a request to the COGE
   service."
  [body]
  (let [paths (:paths (decode-json body))]
    (share-paths paths)
    (parse-genome-viewer-response (request-coge-genome-url paths))))
