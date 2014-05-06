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

(defn get-riak-paths
  [bucket]
  (->> (k/exec-raw icat [tree-urls-query] :results)
       (map :meta_attr_value)
       (filter #(.startsWith % (str "/riak/" bucket "/")))
       (map base-name)))

(def failed-sha1s (atom #{}))

(defn add-failed-sha1 [sha1]
  (reset! failed-sha1s (conj @failed-sha1s sha1)))

(defn tree-urls-url [host port]
  (str "http://" host ":" port))

(defn get-trees-from-riak
  [rb bucket sha1]
  (try
    (-> (key-value rb bucket sha1) json/decode :tree-urls json/encode)
    (catch Exception e
      (println e)
      (println "ERROR GETTING SHA1 FROM RIAK: " sha1)
      (add-failed-sha1 sha1)
      nil)))

(defn post-tree-urls
  [svc rb sha1 bucket]
  (let [_        (println "\t* -- Getting key from" (key-url rb bucket sha1))
        svc-url  (str (url svc sha1))
        _        (println "\t* -- Posting to" svc-url)
        svc-body (get-trees-from-riak rb bucket sha1)]
    (println svc-body)
    (when svc-body
      (let [svc-opts { :body svc-body
                      :body-encode "UTF-8"
                      :content-type "application/json"
                      :throw-exceptions false}
            resp (http/post svc-url svc-opts)]
        (when-not (= 200 (:status resp))
          (println "WARNING, REQUEST FAILED:\n" (:body resp))
          (add-failed-sha1 sha1))
        (when (= 200 (:status resp))
          (println "\t* -- Successfully migrated data for" sha1))))))

(defn tree-urls
  [options]
  (connect-icat options)
  (let [rb (riak-base (:riak-host options) (:riak-port options))
        tree-urls-bucket (:riak-bucket options)
        svc (tree-urls-url (:service-host options) (:service-port options))]
    (println "Migrating tree URLS")
    (println "\t* -- Riak host: " rb)
    (println "\t* -- Service host: " svc)

    (doseq [sha1 (get-riak-paths tree-urls-bucket)]
      (post-tree-urls svc rb sha1 tree-urls-bucket)
      (println ""))

    (println "Tree URL migration failed for the following SHA1s:")
    (doseq [sha1 (seq @failed-sha1s)]
      (println "*" sha1))))
