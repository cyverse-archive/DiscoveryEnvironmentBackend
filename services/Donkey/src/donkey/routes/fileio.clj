(ns donkey.routes.fileio
  (:use [compojure.core]
        [donkey.auth.user-attributes]
        [donkey.util])
  (:require [donkey.util.config :as config]
            [donkey.services.fileio.controllers :as fio]
            [donkey.services.filesystem.directory :as d]
            [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]))

(defn secured-fileio-routes
  "The routes for file IO endpoints."
  []
  (optional-routes
   [config/data-routes-enabled]

   (GET "/fileio/download" [:as {:keys [uri params]}]
        (ce/trap uri #(fio/download params)))

   (POST "/fileio/urlupload" [:as {:keys [uri params body]}]
         (ce/trap uri #(fio/urlupload params body)))

   (POST "/fileio/save" [:as {:keys [uri params body]}]
         (ce/trap uri #(fio/save params body)))

   (POST "/fileio/saveas" [:as {:keys [uri params body]}]
         (ce/trap uri #(fio/saveas params body)))))

(defn unsecured-fileio-routes
  "Routes for FileIO that bypass CAS."
  []
  (optional-routes
   [config/data-routes-enabled]

   (POST "/fileio/upload" [:as {:keys [uri] :as req}]
         (do (log/info "Request: " req)
             (ce/trap uri #(fio/upload (:params req) (:multipart-params req)))))))
