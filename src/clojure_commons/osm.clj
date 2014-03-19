(ns clojure-commons.osm
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure-commons.client :as cc]))

;; Creates a new OSM client.
(defn create [base bucket]
  {:base base
   :bucket bucket})

;; Builds an OSM URL from the base URL, the bucket and the list of components.
(defn- build-url [{:keys [base bucket]} & components]
  (apply cc/build-url base bucket components))

;; Encodes an object or query that is being sent to the OSM.
(defn encode
  [m]
  (cheshire/encode m))

;; Sends a query to the OSM.
(defn query [osm query]
  (:body (client/post (build-url osm "query")
                      {:body (encode query)})))

;; Counts documents matching a query in the OSM.
(defn count-documents [osm query]
  (:body (client/post (build-url osm "count")
                      {:body (encode query)})))

;; Saves an object in the OSM.
(defn save-object [osm obj]
  (:body (client/post (build-url osm) {:body (encode obj)})))

;; Gets an object from the OSM.
(defn get-object [osm id]
  (:body (client/get (build-url osm id))))

;; Updates an object in the OSM.
(defn update-object [osm id obj]
  (:body (client/post (build-url osm id) {:body (encode obj)})))

;; Adds a callback to an object in the OSM.
(defn add-callback [osm id type url]
  (:body (client/post
          (build-url osm id "callbacks")
          {:body (encode {:callbacks [{:type type :callback url}]})})))

;; Removes a calback from an object in the OSM.
(defn remove-callback [osm id type url]
  (:body (client/post
          (build-url osm id "callbacks" "delete")
          {:body (encode {:callbacks [{:type type :callback url}]})})))

;; Gets the list of callbacks from the OSM.
(defn get-callbacks [osm id]
  (:body (client/get (build-url osm id))))
