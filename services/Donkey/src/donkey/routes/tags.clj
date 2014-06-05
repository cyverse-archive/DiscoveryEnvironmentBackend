(ns donkey.routes.tags
  (:use [compojure.core :only [GET PUT PATCH]])
  (:require [donkey.util :as util]
            [donkey.util.config :as config]
            [donkey.util.service :as svc]))


(defn secured-tag-routes
  []
  (util/optional-routes
    [config/metadata-routes-enabled]

    (GET "/filesystem/entry/:entry-id/tags" [:as req]
      (svc/success-response {:tags ["user/username/tag+1" "user/username/tag+2"]}))

    (PATCH "/filesystem/entry/:entry-id/tags" [:as req]
      (svc/success-response))

    (GET "/tags/suggestions" [:as req]
      (svc/success-response {:suggestions ["user/username/tag+1" "user/username/tag+2"]}))

    (PUT "/tags/user/:username/:value" [:as req]
      (svc/success-response))

    (PATCH "/tags/user/:username/:value" [:as req]
      (svc/success-response))))
