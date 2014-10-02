(ns data-info.routes
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [data-info.services.filesystem.home :as home]
            [data-info.services.welcome :as welcome]
            [data-info.util :as util]
            [data-info.util.service :as svc]))


(defroutes routes
  (GET "/" []
    (welcome/welcome))

  (GET "/home" [:as req]
    (util/controller req home/do-homedir :params))

  (route/not-found (svc/unrecognized-path-response)))
