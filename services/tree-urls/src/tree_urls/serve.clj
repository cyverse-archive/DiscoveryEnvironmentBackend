(ns tree-urls.serve
  (:use [kameleon.tree-urls-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response]
        [medley.core])
  (:require [cheshire.core :as json]))

(defn sanitize
  [tree-urls]
  (if tree-urls
    (let [tree-urls? (contains? tree-urls :tree_urls)
          filtered  (filter-keys #(not (contains? #{:id :user_id} %)) tree-urls)]
      (if tree-urls?
        (-> filtered
            (dissoc :tree_urls)
            (assoc :tree_urls (json/parse-string (:tree_urls tree-urls) true)))
        filtered))
    {}))

(defn invalid-uuid
  [uuid]
  {:status 400 :body {:uuid uuid}})

(defn not-a-uuid
  [uuid]
  {:status 404 :body {:uuid uuid}})

(defn invalid-content
  [content-type]
  {:status 415 :body {:content-type content-type}})

(defn uuid?
  [uuid-str]
  (try
    (java.util.UUID/fromString uuid-str)
    true
    (catch Exception e
      false)))

(defmacro validate
  [[uuid req content-type?] & body]
  `(cond
    (not (uuid? ~uuid))
    (invalid-uuid ~uuid)

    (and ~content-type? (not= (:content-type ~req) "application/json"))
    (invalid-content (:content-type ~req))

    :else
    (do ~@body)))

(defn get-req
  [uuid req]
  (validate
   [uuid req false]
   (if-not (tree-urls? uuid)
     (not-a-uuid uuid)
     (response (sanitize (json/parse-string (tree-urls uuid) true))))))

(defn post-req
  [uuid req]
  (validate
   [uuid req true]
   (response (sanitize (save-tree-urls uuid (json/encode (:body req)))))))

(defn delete-req
  [uuid req]
  (validate
   [uuid req false]
   (response (sanitize (delete-tree-urls uuid)))))

(defn put-req
  [uuid req]
  (post-req uuid req))
