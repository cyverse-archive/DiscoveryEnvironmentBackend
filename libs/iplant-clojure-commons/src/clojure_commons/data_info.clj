(ns clojure-commons.data-info
  (:use [clojure-commons.error-codes])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure-commons.client :as cc]))


(defn get-avus
  "Retrieves the AVUs associated with a file."
  [base user path]
  (let [url (cc/build-url base "file" "metadata")
        res (cc/get url {:query-params {:path path
                                        :user user}
                         :as           :json})]
    (get-in res [:body :metadata])))

(defn avu-exists?
  "Determines if an AVU is associated with a file."
  [base user path attr]
  (let [avus (get-avus base user path)]
    (first (filter #(= (:attr %) attr) avus))))

(defn delete-avu
  "Removes an AVU from a file."
  [base user path attr]
  (when (avu-exists? base user path attr)
   (let [url (cc/build-url base "file" "metadata")
         res (cc/delete url {:query-params {:path path
                                            :user user
                                            :attr attr}})]
     (:body res))))

(defn delete-tree-urls
  "Removes all of the tree URLs associated with a file."
  [base user path]
  (delete-avu base user path "tree-urls"))

(defn format-tree-url
  "Creates a tree URL element."
  [label url]
  {:label label
   :url   url})

(defn format-tree-urls
  "Formats the tree URLs for storage in the file metadata.  The urls argument
   should contain a sequence of elements as returned by format-tree-url."
  [urls]
  {:tree-urls urls})

(defn- tree-metaurl-url
  "Builds a URL that can be used to get or save a URL that can be used to
   retrieve a list of tree URLs."
  [base]
  (cc/build-url base "metadata"))

(defn tree-metaurl-body
  "Builds the body of request to store a tree metaurl in an AVU."
  [metaurl]
  (cheshire/encode {:attr  "tree-urls"
                    :value metaurl
                    :unit  ""}))

(defn save-tree-metaurl
  "Saves the URL used to get saved tree URLs.  The metaurl argument should
   contain the URL used to obtain the tree URLs."
  [base user path metaurl]
  (cc/post (tree-metaurl-url base)
           {:body             (tree-metaurl-body metaurl)
            :content-type     :json
            :query-params     {:path path
                               :user user}
            :throw-exceptions false}))

(defn get-tree-metaurl
  "Gets the URL used to get saved tree URLs."
  [base user path]
  (let [res (client/get (tree-metaurl-url base) {:throw-exceptions false})]
    (when (<= 200 (:status res) 299)
      (->> (:body res)
           (#(cheshire/decode % true))
           (:metadata)
           (:filter #(= (:attr %) "tree-urls"))
           (first)
           (:value)))))

(defn get-user-groups
  "Retrieves the set of groups a user belongs to."
  [base user]
  (let [resp (cc/get (cc/build-url base "groups")
                     {:query-params {:user user} :as :json})]
    (set (get-in resp [:body :groups]))))
