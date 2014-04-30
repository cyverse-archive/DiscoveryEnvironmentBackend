(ns tree-urls.serve
  (:use [kameleon.tree-urls-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response]
        [medley.core])
  (:require [cheshire.core :as json]))

(defn filter-result
  [result]
  (if (map? result)
    (filter-keys #(not (contains? #{:id :sha1 :user_id} %)) result)
    result))

(defn sanitize
  [tree-urls]
  (if tree-urls
    (filter-result tree-urls)
    {}))

(defn sha1-format?
  [sha1]
  (cond
   (not= (count sha1) 40)
   false

   (not (re-find #"^[a-fA-F0-9]+$" sha1))
   false

   :else
   true))

(defn not-sha1
  [sha1]
  {:status 400 :body (str "Invalid SHA1 format: " sha1)})

(defn sha1-not-present
  [sha1]
  {:status 404 :body (str "Not Found: " sha1)})

(defn invalid-content
  [content-type]
  {:status 415 :body (str "Invalid content type: " content-type)})

(defmacro validate
  [[sha1 req content-type?] & body]
  `(cond
    (not (sha1-format? ~sha1))
    (not-sha1 ~sha1)

    (and ~content-type? (not= (:content-type ~req) "application/json"))
    (invalid-content (:content-type ~req))

    :else
    (do ~@body)))

(defn get-req
  [sha1 req]
  (validate
   [sha1 req false]
   (if-not (tree-urls? sha1)
     (sha1-not-present sha1)
     (response (sanitize (json/parse-string (tree-urls sha1) true))))))

(defn post-req
  [sha1 req]
  (validate
   [sha1 req true]
   (response (sanitize (save-tree-urls sha1 (json/encode (:body req)))))))

(defn delete-req
  [sha1 req]
  (validate
   [sha1 req false]
   (if-not (tree-urls? sha1)
     ""
     (do (delete-tree-urls sha1)
         ""))))

(defn put-req
  [sha1 req]
  (post-req sha1 req))
