(ns donkey.routes.fileio
  (:use [compojure.core]
        [donkey.auth.user-attributes])
  (:require [donkey.util.config :as config]
            [donkey.services.fileio.controllers :as fio]
            [clojure.tools.logging :as log]
            [ring.middleware.multipart-params :as multipart]
            [donkey.util :as util]))


(defn secured-fileio-routes
  "The routes for file IO endpoints."
  []
  (util/optional-routes [config/data-routes-enabled]

    (GET "/fileio/download" [:as {:keys [params]}]
      (fio/download params))

    (POST "/fileio/upload" [:as req]
      (let [req' (multipart/multipart-params-request req {:store fio/store-irods})]
        (fio/upload (:params req'))))

    (POST "/fileio/urlupload" [:as {:keys [params body]}]
      (fio/urlupload params body))

    (POST "/fileio/save" [:as {:keys [params body]}]
      (fio/save params body))

    (POST "/fileio/saveas" [:as {:keys [params body]}]
      (fio/saveas params body))))


(defn unsecured-fileio-routes
  "Routes for FileIO that bypass CAS."
  []
  (util/optional-routes [config/data-routes-enabled]

    (POST "/fileio/upload" [:as req]
      (let [req' (multipart/multipart-params-request req {:store fio/store-irods})]
        (log/info "Request: " req')
        (fio/unsecured-upload (:params req') (:multipart-params req'))))))
