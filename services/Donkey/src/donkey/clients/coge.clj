(ns donkey.clients.coge
  (:use [donkey.auth.user-attributes :only [current-user]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]
            [donkey.util.jwt :as jwt]))

(defn- coge-url
  [& components]
  (str (apply curl/url (config/coge-base-url) components)))

(defn- coge-auth-header
  ([user]
     (coge-auth-header user {}))
  ([user headers]
     (assoc headers
       :X-IPlant-DE-JWT (jwt/generate-jwt user))))

(defn- default-error-handler
  [error-code {:keys [body] :as response}]
  (log/warn "CoGe request failed:" response)
  (throw+ {:error_code error-code
           :reason     (if (string? body) body (slurp body))}))

(defmacro ^:private with-trap
  [[handle-error] & body]
  `(try+
    (do ~@body)
    (catch [:status 400] bad-request#
      (~handle-error ce/ERR_BAD_REQUEST bad-request#))
    (catch [:status 404] not-found#
      (~handle-error ce/ERR_NOT_FOUND not-found#))
    (catch (comp number? :status) server-error#
      (~handle-error ce/ERR_REQUEST_FAILED server-error#))))

(def test-organism-id 38378)

(defn- genome-viewer-url-request
  "Builds the request to import a list of genomes into CoGe."
  [paths]
  (cheshire/encode
   {:organism_id test-organism-id
    :metadata    {:name        "test"
                  :version     "1"
                  :type_id     1
                  :source_name "test"
                  :restricted  true}
    :source_data (map (partial hash-map :type "irods" :path) paths)}))

(defn get-genome-viewer-url
  "Sends a request for a genome viewer URL to the COGE service."
  [paths]
  (with-trap [default-error-handler]
    (let [request-url (coge-url "genomes")]
      (:body (http/put request-url {:body         (genome-viewer-url-request paths)
                                    :headers      (coge-auth-header current-user)
                                    :content-type :json
                                    :as           :json})))))
