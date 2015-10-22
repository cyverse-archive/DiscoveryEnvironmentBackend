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

;; NAVIGATION

(defn list-roots
  "Uses the data-info navigation/root endpoint to list a user's navigation roots."
  [user]
  (let [url (url/url (cfg/data-info-base) "navigation" "root")
        req-map {:query-params {:user user}
                 :content-type :json}]
    (http/get (str url) req-map)))

(defn- mk-nav-url
  [path]
  (let [nodes         (fs/split path)
        nodes         (if (= "/" (first nodes)) (next nodes) nodes)
        encoded-nodes (map url/url-encode nodes)]
    (apply url/url (cfg/data-info-base) "navigation" "path" encoded-nodes)))

(defn list-directories
  "Uses the data-info navigation/path endpoint to list directories contained under path."
  [user path]
  (http/get (str (mk-nav-url path))
            {:query-params {:user user}
             :content-type :json}))

;; READ

(defn read-chunk
  "Uses the data-info read-chunk endpoint."
  [user path-uuid position chunk-size]
  (let [url (url/url (cfg/data-info-base-url) "data" path-uuid "chunks")
        req-map {:query-params
                  {:user user
                   :position position
                   :size chunk-size}
                 :content-type :json}]
    (http/get (str url) req-map)))

(defn read-tabular-chunk
  "Uses the data-info read-tabular-chunk endpoint."
  [user path-uuid separator page chunk-size]
  (let [url (url/url (cfg/data-info-base-url) "data" path-uuid "chunks-tabular")
        req-map {:query-params
                  {:user user
                   :separator separator
                   :page page
                   :size chunk-size}
                 :content-type :json}]
    (http/get (str url) req-map)))

;; CREATE

(defn create-dirs
  "Uses the data-info directories endpoint to create several directories."
  [user paths]
  (let [url     (url/url (cfg/data-info-base-url) "data" "directories")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:paths paths})}]
    (http/post (str url) req-map)))

;; MOVE AND RENAME

(defn rename
  "Uses the data-info set-name endpoint to rename a file within the same directory."
  [user path-uuid new-name]
  (let [url (url/url (cfg/data-info-base-url) "data" path-uuid "name")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:filename new-name})}]
    (http/put (str url) req-map)))

(defn move-single
  "Uses the data-info single-item directory change endpoint to move an item to a different directory."
  [user path-uuid dest]
  (let [url (url/url (cfg/data-info-base-url) "data" path-uuid "dir")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:dirname dest})}]
    (log/info (str "using " (str url) " to move data item " path-uuid " to " dest))
    (http/put (str url) req-map)))

(defn move-multi
  "Uses the data-info bulk mover endpoint to move a number of items to a different directory."
  [user sources dest]
  (let [url (url/url (cfg/data-info-base-url) "mover")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:sources sources :dest dest})}]
    (log/info (str "using " (str url) " to move several data items to " dest))
    (http/post (str url) req-map)))

(defn move-contents
  "Uses the data-info set-children-directory-name endpoint to move the contents of one directory
   into another directory."
  [user path-uuid dest]
  (let [url (url/url (cfg/data-info-base-url) "data" path-uuid "children" "dir")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:dirname dest})}]
    (http/put (str url) req-map)))

;; DELETION

(defn delete-paths
    "Uses the data-info deleter endpoint to delete many paths."
    [user paths]
    (let [url (url/url (cfg/data-info-base-url) "deleter")
          req-map {:query-params {:user user}
                   :content-type :json
                   :body (json/encode {:paths paths})}]
      (http/post (str url) req-map)))

(defn delete-contents
    "Uses the data-info delete-children endpoint to delete the contents of a directory."
    [user path-uuid]
    (let [url (url/url (cfg/data-info-base-url) "data" path-uuid "children")
          req-map {:query-params {:user user}
                   :content-type :json}]
      (http/delete (str url) req-map)))

(defn delete-trash
    "Uses the data-info trash endpoint to empty the trash of a user."
    [user]
    (let [url (url/url (cfg/data-info-base-url) "/trash")
          req-map {:query-params {:user user}
                   :content-type :json}]
      (http/delete (str url) req-map)))

(defn restore-files
    "Uses the data-info restorer endpoint to restore many or all paths."
    ([user]
     (restore-files user []))
    ([user paths]
     (let [url (url/url (cfg/data-info-base-url) "/restorer")
           req-map {:query-params {:user user}
                    :content-type :json
                    :body         (json/encode {:paths paths})}]
       (http/post (str url) req-map))))

;; MISC

(defn collect-permissions
  "Uses the data-info permissions-gatherer endpoint to query user permissions for a set of files/folders."
  [user paths]
  (let [url (url/url (cfg/data-info-base-url) "permissions-gatherer")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body (json/encode {:paths paths})}]
    (http/post (str url) req-map)))

(defn collect-stats
  "Uses the data-info stat-gatherer endpoint to gather stat information for a set of files/folders."
  [user paths]
  (let [url     (url/url (cfg/data-info-base) "stat-gatherer")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:paths paths})}]
    (http/post (str url) req-map)))

(defn check-existence
  "Uses the data-info existence-marker endpoint to gather existence information for a set of files/folders."
  [user paths]
  (let [url     (url/url (cfg/data-info-base) "existence-marker")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:paths paths})}]
    (http/post (str url) req-map)))

(defn get-type-list
  "Uses the data-info file-types endpoint to produce a list of acceptable types."
  []
  (let [url (url/url (cfg/data-info-base-url) "file-types")
        req-map {:content-type :json}]
    (http/get (str url) req-map)))

(defn set-file-type
  "Uses the data-info set-type endpoint to change the type of a file."
  [user path-uuid type]
  (let [url (url/url (cfg/data-info-base-url) "data" path-uuid "type")
        req-map {:query-params {:user user}
                 :content-type :json
                 :body (json/encode {:type type})}]
    (http/put (str url) req-map)))
