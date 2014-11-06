(ns data-info.services.cart
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [cheshire.core :as json]
            [liberator.core :refer [defresource]]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.cart :as cart]
            [clj-jargon.init :as init]
            [clj-jargon.item-info :as item]
            [clj-jargon.users :as user]
            [clojure-commons.error-codes :as error]
            [data-info.util.config :as cfg]
            [data-info.util.paths :as path])
  (:import [com.fasterxml.jackson.core JsonParseException]))


(defn- fmt-media-type
  [type-str]
  (when type-str
    {:media-type (-> type-str string/trim string/lower-case)}))


(defn- fmt-ows
  [ows-str]
  (let [[parameter value] (string/split (string/trim ows-str) #"=" 2)
        parameter         (keyword (string/lower-case parameter))
        value             (string/lower-case (string/replace value #"^\"(.*)\"$" "$1"))]
    {parameter value}))


(defn- parse-content-type
  [type-str]
  (let [[media-type & owss] (when type-str (string/split type-str #";"))]
    (apply merge (fmt-media-type media-type) (map fmt-ows owss))))


(defn body-reader
  [body encoding]
  (if encoding
    (io/reader body :encoding (string/upper-case encoding))
    (io/reader body)))


(defn- known-content-type?
  [ctx]
  (try
    (let [content-type (parse-content-type (get-in ctx [:request :headers "content-type"]))
          media-type   (:media-type content-type)
          content      (when-let [body (get-in ctx [:request :body])]
                         (json/decode-stream (body-reader body (:charset media-type)) true))]
      (when (or (nil? media-type) (= media-type "application/json"))
        {::content content}))
    (catch JsonParseException _)))


(defn- malformed?
  [user _]
  (when-not user
    {:representation {:media-type "application/json"}
     :message        {:error_code error/ERR_MISSING_QUERY_PARAMETER :parameters [:user]}}))


(defn- folder-processable?
  [cm folder]
  (try
    (or (not folder) (item/is-dir? cm folder))
    (catch Throwable _)))


(defn- processable?
  [user folder {body ::content}]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (and (user/user-exists? cm user)
         (folder-processable? cm folder)
         (or (not body)
             (boolean (:paths body))))))


(defn- mk-cart
  [cart-key user password]
  {:key                    cart-key
   :user                   user
   :home                   (path/user-home-dir user)
   :password               password
   :host                   (cfg/irods-host)
   :port                   (read-string (cfg/irods-port))
   :zone                   (cfg/irods-zone)
   :defaultStorageResource (cfg/irods-resc)})


(defn- post!
  [user folder {{other-paths :paths} ::content}]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (let [cart-key     (str (System/currentTimeMillis))
          folder-paths (when folder (icat/folder-path-listing user (cfg/irods-zone) folder))
          all-paths    (set (concat other-paths folder-paths))
          password     (if (empty? all-paths)
                         (cart/temp-password cm user)
                         (cart/store-cart cm user cart-key all-paths))]
      {::cart (mk-cart cart-key user password)})))


(defn- handle-unprocessable-entity
  [user folder _]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (let [[ec fields] (cond
                        (not (user/user-exists? cm user))     [error/ERR_NOT_A_USER []]
                        (not (folder-processable? cm folder)) [error/ERR_NOT_A_FOLDER []]
                        :else                                 [error/ERR_BAD_OR_MISSING_FIELD
                                                               [:paths]])]
      {:error_code ec
       :fields     fields
       :path       folder
       :user       user})))


(defresource cart [user folder]
  :allowed-methods             [:post]
  :available-media-types       ["application/json"]
  :known-content-type?         known-content-type?
  :malformed?                  (partial malformed? user)
  :processable?                (partial processable? user folder)
  :post!                       (partial post! user folder)
  :handle-created              #(::cart %)
  :handle-malformed            #(json/encode (:message %))
  :handle-unprocessable-entity (partial handle-unprocessable-entity user folder))
