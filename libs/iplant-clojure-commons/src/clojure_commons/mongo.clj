(ns clojure-commons.mongo
  (:require [cheshire.core :as cheshire])
  (:import [com.mongodb Mongo DB DBCollection BasicDBObject DBCursor]))

;;; Object translations
(defn map->obj
  "Translates a clojure json structure to a mongodb object."
  [my-map]
  (let [result (BasicDBObject.)]
    (doseq [key (keys my-map)]
      (let [value (get my-map key)]
        (.put result
          (name key)
          (cond
            (map? value)        (map->obj value)
            (sequential? value) (if-not (map? (first value))
                                  value
                                  (doall (map map->obj value)))
            :else               value))))
    result))

(defn obj->map
  "Translates a mongodb object to clojure json"
  [mongo-obj]
  (when-not (nil? mongo-obj)
    (into {} (cheshire/decode (str mongo-obj) true))))

;;; More clojure-esque names
(defn make-connection [host port]
  (Mongo. host port))

(defn get-database [conn db-name]
  (.getDB conn db-name))

(defn get-collection [db collection-name]
  (.getCollection db collection-name))

(defn mongo-count
  [collection query-json]
  (.count collection (map->obj query-json)))

(defn mongo-find
  "Takes in json and finds a mongodb object based on that json.  Equivallent
   to the regular mongo client db.collection.find()"
  ([collection query-json]
     (doall (map obj->map (.toArray (.find collection (map->obj query-json))))))

  ([collection query-map fields-map]
     (let [query  (map->obj query-map)
	   fields (map->obj fields-map)]
       (doall (map obj->map (.toArray (.find collection query fields)))))))

(defn mongo-find-and-modify
  "Takes in a collection, finds the mongodb object based on the query-map
   and replaces the first result with the object created with update-map."
  ([collection query-map update-map]
     (let [query (map->obj query-map)
	   update (map->obj update-map)]
       (obj->map (.findAndModify collection query update))))

  ([collection query-map sort-map update-map]
     (let [query (map->obj query-map)
	   sort (map->obj sort-map)
	   update (if (not (nil? update-map)) (map->obj update-map))]
       (obj->map (.findAndModify collection query sort update)))))

(defn mongo-insert
  [collection insert-map]
  (let [write-concern (.getWriteConcern collection)
	insert (map->obj insert-map)]
    (.throwOnError (.getLastError (.insert collection insert write-concern)))))
