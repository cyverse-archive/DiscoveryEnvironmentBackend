(ns donkey.routes.admin
  (:use [compojure.core]
        [donkey.auth.user-attributes]
        [donkey.util])
  (:require [donkey.util.config :as config]
            [donkey.services.admin :as admin]
            [clojure.tools.logging :as log]))

(defn secured-admin-routes
  "The routes for the admin endpoints."
  []
  (optional-routes
    [config/admin-routes-enabled]
    
    (GET "/admin/config" [:as req]
         (trap #(admin/config)))
    
    (GET "/admin/status" [:as req]
         (trap #(admin/status req)))))
