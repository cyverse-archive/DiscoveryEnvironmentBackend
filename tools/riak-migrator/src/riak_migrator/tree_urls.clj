(ns riak-migrator.tree-urls
  (:use [riak-migrator.riak]
        [kameleon.pgpass]
        [kameleon.uuids])
  (:require [clj-http.client :as http]
            [korma.db :as db]
            [korma.core :as k]
            [cemerick.url :refer [url url-encode]]
            [clojure.string :as string]
            [me.raynes.fs :refer [base-name]]
            [cheshire.core :as json]))

(defn icat-db-spec
  "Creates a Korma db spec for the ICAT."
  [hostname user pass & {:keys [port db]
                         :or {port 5432
                              db "ICAT"}}]
  (db/postgres {:host     hostname
                :port     port
                :db       db
                :user     user
                :password pass}))

(defn setup-icat
  "Defines the icat database. Pass in the return value of icat-db-spec."
  [icat-db-spec]
  (db/defdb icat icat-db-spec))

(defn connect-icat
  [options]
  (let [icat-host (:icat-host options)
        icat-port (:icat-port options)
        icat-user (:icat-user options)
        icat-name (:icat-name options)
        passwd    (get-password icat-host icat-port icat-name icat-user)]
    (setup-icat (icat-db-spec icat-host icat-user passwd :port icat-port :db icat-name))))

(def tree-urls-query
  "select meta_attr_value
     from r_meta_main
    where meta_attr_name = 'tree-urls';")

(defn hyphenate
  "Adapted from http://stackoverflow.com/a/18992809"
  [unhyphenated]
  (clojure.string/replace unhyphenated #"(\w{8})(\w{4})(\w{4})(\w{4})(\w{12})" "$1-$2-$3-$4-$5"))

(defn get-riak-paths
  []
  (->> (k/exec-raw icat [tree-urls-query] :results)
       (map :meta_attr_value)
       (filter #(.startsWith % "/riak/tree-urls/"))
       (map base-name)))

(def failed-uuids (atom #{}))

(defn add-failed-uuids [uuid]
  (reset! failed-uuids (conj @failed-uuids uuid)))

(def tree-urls-bucket "tree-urls")

(defn tree-urls-url [host port]
  (str "http://" host ":" port))

(defn get-trees-from-riak
  [rb bucket uuid]
  (json/encode (:tree-urls (json/decode (key-value rb bucket uuid) true))))

(defn post-tree-urls
  [svc rb uuid bucket]
  (let [_        (println "\t* -- Getting key from" (key-url rb bucket uuid))
        svc-url  (str (url svc (hyphenate uuid)))
        _        (println "\t* -- Posting to" svc-url)
        svc-body (get-trees-from-riak rb bucket uuid)
        _        (println "\t* -- Body of request\n" svc-body)
        svc-opts {:body svc-body
                  :body-encode "UTF-8"
                  :content-type "application/json"
                  :throw-exceptions true}
        resp     (http/post svc-url svc-opts)]
    (when-not (= 200 (:status resp))
      (println "WARNING, REQUEST FAILED:\n" (:body resp))
      (add-failed-uuids uuid))
    (when (= 200 (:status resp))
      (println "\t* -- Successfully migrated data for" uuid))))

(defn tree-urls
  [options]
  (connect-icat options)
  (let [rb  (riak-base (:riak-host options) (:riak-port options))
        svc (tree-urls-url (:service-host options) (:service-port options))]
    (println "Migrating tree URLS")
    (println "\t* -- Riak host: " rb)
    (println "\t* -- Service host: " svc)

    (doseq [uuid (get-riak-paths)]
      (println uuid)
      (post-tree-urls svc rb uuid tree-urls-bucket))))
