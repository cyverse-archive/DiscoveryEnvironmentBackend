(ns data-info.routes.welcome
  (:use [compojure.core])
  (:require [data-info.util :as util]
            [data-info.services.welcome :as svc]))


(defn route
  "The top-level route"
  []
  (GET "/" [:as req]
    (util/controller req svc/welcome)))
