(ns donkey.routes.tree-viewer
  (:use [compojure.core]
        [donkey.services.buggalo]
        [donkey.util.service]
        [donkey.auth.user-attributes]
        [donkey.util])
  (:require [donkey.util.config :as config]
            [clojure.tools.logging :as log]))

(defn secured-tree-viewer-routes
  []
  (optional-routes
   [config/tree-viewer-routes-enabled]

   (GET "/tree-viewer-urls" [:as {params :params}]
        (trap #(log/spy (tree-viewer-urls
                         (required-param params :path)
                         (:shortUsername current-user)
                         params))))))

(defn unsecured-tree-viewer-routes
  []
  (optional-routes
   [config/tree-viewer-routes-enabled]

   (POST "/tree-viewer-urls" [:as {body :body params :params}]
         (trap #(tree-viewer-urls-for body params)))))
