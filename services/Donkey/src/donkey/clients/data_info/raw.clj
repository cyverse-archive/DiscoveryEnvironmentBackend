(ns donkey.clients.data-info.raw
  (:require [clojure.tools.logging :as log]
            [cemerick.url :as url]
            [me.raynes.fs :as fs]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [donkey.util.config :as cfg])
  (:import [clojure.lang IPersistentMap ISeq Keyword]))

;; HELPER FUNCTIONS

(defn- resolve-http-call
  [method]
  (case method
    :delete   http/delete
    :get      http/get
    :head     http/head
    :options  http/options
    :patch    http/patch
    :post     http/post
    :put      http/put))

(defn request
  "This function makes an HTTP request to the data-info service. It uses clj-http to make the
   request."
  [^Keyword method ^ISeq url-path ^IPersistentMap req-map]
  (let [url (apply url/url (cfg/data-info-base) url-path)]
    ((resolve-http-call method) (str url) req-map)))

(defn- add-user
  [req-map user]
  (assoc-in req-map [:query-params :user] user))

(defn- add-body
  [req-map body]
  (assoc req-map :body body))

(defn- add-extra-params
  [req-map extra-params]
  (assoc req-map
         :query-params (merge (get req-map :query-params {})
                              extra-params)))

(defn- mk-req-map
  ([]
    {:content-type :json})
  ([user]
    (add-user (mk-req-map) user))
  ([user item]
    (-> (if (= (type item) String)
          (add-body (mk-req-map) item)
          (add-extra-params {} item))
        (add-user user)))
  ([user body extra-params]
    (-> (add-user (mk-req-map) user)
        (add-body body)
        (add-extra-params extra-params))))

;; NAVIGATION

(defn list-roots
  "Uses the data-info navigation/root endpoint to list a user's navigation roots."
  [user]
  (request :get ["navigation" "root"] (mk-req-map user)))

(defn- mk-nav-url
  [path]
  (let [nodes         (fs/split path)
        nodes         (if (= "/" (first nodes)) (next nodes) nodes)
        encoded-nodes (map url/url-encode nodes)]
    (apply url/url (cfg/data-info-base) "navigation" "path" encoded-nodes)))

(defn list-directories
  "Uses the data-info navigation/path endpoint to list directories contained under path."
  [user path]
  (http/get (str (mk-nav-url path)) (mk-req-map user)))

;; READ

(defn read-chunk
  "Uses the data-info read-chunk endpoint."
  [user path-uuid position chunk-size]
  (request :get ["data" path-uuid "chunks"]
           (mk-req-map user
                  {:position position
                   :size chunk-size})))

(defn read-tabular-chunk
  "Uses the data-info read-tabular-chunk endpoint."
  [user path-uuid separator page chunk-size]
  (request :get ["data" path-uuid "chunks-tabular"]
           (mk-req-map user
                  {:separator separator
                   :page page
                   :size chunk-size})))

;; CREATE

(defn create-dirs
  "Uses the data-info directories endpoint to create several directories."
  [user paths]
  (request :post ["data" "directories"]
           (mk-req-map user (json/encode {:paths paths}))))

;; MOVE AND RENAME

(defn rename
  "Uses the data-info set-name endpoint to rename a file within the same directory."
  [user path-uuid new-name]
  (request :put ["data" path-uuid "name"]
           (mk-req-map user (json/encode {:filename new-name}))))

(defn move-single
  "Uses the data-info single-item directory change endpoint to move an item to a different directory."
  [user path-uuid dest]
  (log/info (str "using move-single to move data item " path-uuid " to " dest))
  (request :put ["data" path-uuid "dir"]
           (mk-req-map user (json/encode {:dirname dest}))))

(defn move-multi
  "Uses the data-info bulk mover endpoint to move a number of items to a different directory."
  [user sources dest]
  (log/info (str "using move-multi to move several data items to " dest))
  (request :post ["mover"]
           (mk-req-map user (json/encode {:sources sources :dest dest}))))

(defn move-contents
  "Uses the data-info set-children-directory-name endpoint to move the contents of one directory
   into another directory."
  [user path-uuid dest]
  (request :put ["data" path-uuid "children" "dir"]
           (mk-req-map user (json/encode {:dirname dest}))))

;; DELETION

(defn delete-paths
    "Uses the data-info deleter endpoint to delete many paths."
    [user paths]
    (request :post ["deleter"]
             (mk-req-map user (json/encode {:paths paths}))))

(defn delete-contents
    "Uses the data-info delete-children endpoint to delete the contents of a directory."
    [user path-uuid]
    (request :delete ["data" path-uuid "children"]
             (mk-req-map user)))

(defn delete-trash
    "Uses the data-info trash endpoint to empty the trash of a user."
    [user]
    (request :delete ["trash"] (mk-req-map user)))

(defn restore-files
    "Uses the data-info restorer endpoint to restore many or all paths."
    ([user]
     (restore-files user []))
    ([user paths]
     (request :post ["restorer"]
              (mk-req-map user (json/encode {:paths paths})))))

;; MISC

(defn collect-permissions
  "Uses the data-info permissions-gatherer endpoint to query user permissions for a set of files/folders."
  [user paths]
  (request :post ["permissions-gatherer"]
           (mk-req-map user (json/encode {:paths paths}))))

(defn collect-stats
  "Uses the data-info stat-gatherer endpoint to gather stat information for a set of files/folders."
  [user paths]
  (request :post ["stat-gatherer"]
           (mk-req-map user (json/encode {:paths paths}))))

(defn check-existence
  "Uses the data-info existence-marker endpoint to gather existence information for a set of files/folders."
  [user paths]
  (request :post ["existence-marker"]
           (mk-req-map user (json/encode {:paths paths}))))

(defn get-type-list
  "Uses the data-info file-types endpoint to produce a list of acceptable types."
  []
  (request :get ["file-types"] (mk-req-map)))

(defn set-file-type
  "Uses the data-info set-type endpoint to change the type of a file."
  [user path-uuid type]
  (request :put ["data" path-uuid "type"]
           (mk-req-map user (json/encode {:type type}))))
