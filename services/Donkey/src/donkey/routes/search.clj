(ns donkey.routes.search
  "the routing code for search-related URL resources"
  (:use [clojure-commons.error-codes :only [missing-arg-response]]
        [compojure.core :only [GET]])
  (:require [donkey.auth.user-attributes :as user]
            [donkey.services.search :as search]
            [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as svc]))


(defn secured-search-routes
  "The routes for search-related endpoints."
  []
  (util/optional-routes
    [config/search-routes-enabled]

    (GET "/filesystem/index" [q tags & opts]
      (if (or q tags)
        (search/search (search/qualify-name (:shortUsername user/current-user)) q tags opts)
        (missing-arg-response "`q` or `tags`")))))
