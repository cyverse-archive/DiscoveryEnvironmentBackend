(ns donkey.routes.admin
  (:use [compojure.core]
        [donkey.auth.user-attributes]
        [donkey.util])
  (:require [donkey.util.config :as config]
            [donkey.services.admin :as admin]
            [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]))

(defn secured-admin-routes
  "The routes for the admin endpoints."
  []
  (optional-routes
    [config/admin-routes-enabled]

    (GET "/admin/config" [:as {:keys [uri] :as req}]
         (ce/trap uri #(admin/config)))

    (GET "/admin/status" [:as {:keys [uri] :as req}]
         (ce/trap uri #(admin/status req)))))
