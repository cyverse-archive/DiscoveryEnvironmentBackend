(ns donkey.routes.fileio
  (:use [compojure.core]
        [donkey.auth.user-attributes]
        [donkey.util])
  (:require [donkey.util.config :as config]
            [donkey.services.fileio.controllers :as fio]
            [donkey.services.filesystem.directory :as d]
            [clojure.tools.logging :as log]))

(defn secured-fileio-routes
  "The routes for file IO endpoints."
  []
  (optional-routes
   [config/data-routes-enabled]

   (GET "/fileio/download" [:as req]
        (trap #(fio/download (:params req))))

   (POST "/fileio/urlupload" [:as req]
           (trap #(fio/urlupload (:params req) (:body req))))

    (POST "/fileio/save" [:as req]
        (trap #(fio/save (:params req) (:body req))))

   (POST "/fileio/saveas" [:as req]
        (trap #(fio/saveas (:params req) (:body req))))))

(defn unsecured-fileio-routes
  "Routes for FileIO that bypass CAS."
  []
  (optional-routes
    [config/data-routes-enabled]

    (POST "/fileio/upload" [:as req]
          (do (log/info "Request: " req)
            (trap #(fio/upload (:params req) (:multipart-params req)))))))
