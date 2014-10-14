(ns data-info.services.cart
  (:require [dire.core :refer [with-pre-hook! with-post-hook!]]
            [slingshot.slingshot :refer [throw+]]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.cart :as cart]
            [clj-jargon.init :refer [with-jargon]]
            [clojure-commons.error-codes :as error]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.validators :as validators]
            [data-info.services.common-paths :as path]
            [data-info.services.directory :as directory]))


(defn- gather-paths
  [cm user folder other-paths]
  (when folder
    (validators/path-is-dir cm folder))
  (set (concat other-paths
               (when folder
                 (directory/get-paths-in-folder user folder)))))


(defn- mk-cart
  [cart-key user password]
  {:key                    cart-key
   :user                   user
   :home                   (path/user-home-dir user)
   :password               password
   :host                   (cfg/irods-host)
   :port                   (cfg/irods-port)
   :zone                   (cfg/irods-zone)
   :defaultStorageResource (cfg/irods-resc)})


(defn dispatch-cart
  [{folder :folder user :user} {other-paths :paths}]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [paths    (gather-paths cm user folder other-paths)
          cart-key (str (System/currentTimeMillis))
          password (if (empty? paths)
                     (cart/temp-password cm user)
                     (cart/store-cart cm user cart-key paths))]
      {:cart (mk-cart cart-key user password)})))


(with-pre-hook! #'dispatch-cart
  (fn [params body]
    (dul/log-call "dispatch-cart" params body)
    (cv/validate-query-params params {:user string?})
    (when body
      (cv/validate-map body {:paths sequential?}))))

(with-post-hook! #'dispatch-cart (dul/log-func "dispatch-cart"))
