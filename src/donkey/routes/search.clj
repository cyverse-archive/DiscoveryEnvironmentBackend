(ns donkey.routes.search
  "the routing code for search-related URL resources"
  (:use [compojure.core :only [GET]])
  (:require [cheshire.core :as json]
            [donkey.auth.user-attributes :as user]
            [donkey.services.search :as search]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as svc]))


(defn secured-search-routes
  "The routes for search-related endpoints."
  []
  (util/optional-routes
    [config/search-routes-enabled]

    (GET "/filesystem/index" [q & opts]
      (if q
        (search/search (search/qualify-name (:shortUsername user/current-user))
                       (json/parse-string q)
                       opts)
        (svc/missing-arg-response "q")))))
