(ns data-info.routes.legacy
  (:require [compojure.api.legacy :refer [GET HEAD POST]]
            [compojure.api.sweet :refer [defroutes*]]
            [compojure.route :as route]
            [data-info.services.cart :as cart]
            [data-info.services.entry :as entry]
            [data-info.util.service :as svc]))


(defroutes* all-routes
  (POST "/cart" [user folder]                             (cart/cart user folder))
  (route/not-found (svc/unrecognized-path-response)))
