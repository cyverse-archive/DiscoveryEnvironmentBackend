(ns donkey.routes.tags
  (:use [compojure.core :only [GET PUT PATCH]])
  (:require [donkey.util.config :as config]
            [donkey.util :as util]))


(defn secured-tag-routes
  []
  (util/optional-routes
    [config/metadata-routes-enabled]

    (GET "/filesystem/entry/:entry-id/tags" [:as req]
      {:tags ["user/username/tag+1" "user/username/tag+2"]})

    (PATCH "/filesystem/entry/:entry-id/tags" [:as req]
      nil)

    (GET "/tags/suggestions" [:as req]
      {:suggestions ["user/username/tag+1" "user/username/tag+2"]})

    (PUT "/tags/user/:username/:value" [:as req]
      nil)

    (PATCH "/tags/user/:username/:value" [:as req]
      nil)))
