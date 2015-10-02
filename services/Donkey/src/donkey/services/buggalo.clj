(ns donkey.services.buggalo
  (:use [clojure.java.io :only [copy file]]
        [clojure-commons.file-utils :only [with-temp-dir-in]]
        [donkey.util.config :only [tree-parser-url]]
        [donkey.services.buggalo.nexml :only [is-nexml? extract-trees-from-nexml]]
        [donkey.util.service :only [success-response temp-dir-failure-response]]
        [donkey.auth.user-attributes :only [current-user]]
        [donkey.clients.tree-urls]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.data-info :as di]
            [donkey.services.fileio.actions :as fileio]
            [donkey.util.tree-url :as tu])
  (:import [java.security MessageDigest DigestInputStream]
           [org.forester.io.parsers.util ParserUtils PhylogenyParserException]
           [org.forester.io.writers PhylogenyWriter]
           [org.forester.phylogeny PhylogenyMethods Phylogeny]
           [java.io File]))

(defn- metaurl-for
  "Builds the meta-URL for to use when saving tree files the database.  The SHA1 hash
   of the contents of the tree file is used as the key in the database."
  [sha1]
  (->> ["/riak/tree-urls" sha1]
       (map #(string/replace % #"^/|/$" ""))
       (string/join "/")))

(defn- temp-dir-creation-failure
  "Handles the failure to create a temporary directory."
  [parent prefix base]
  (log/error "failed to create a temporary directory: base =" base)
  (temp-dir-failure-response {:parent parent
                              :prefix prefix
                              :base   base}))

(defn- tree-parser-error
  "Throws an exception indicating that the tree parser encountered an error."
  [res]
  (log/error "the tree parser encountered an error:" (:body res))
  (let [body {:action  "tree_manifest"
              :message "unable to parse tree data"
              :details (:body res)
              :success false}]
    (throw+ {:status       (:status res)
             :content-type :json
             :body         (cheshire/generate-string body)})))

(defn- get-tree-viewer-url
  "Obtains a tree viewer URL for a single tree file."
  [^File f]
  (log/debug "obtaining a tree viewer URL for" (.getName f))
  (let [label     (string/replace (.getName f) #"[.]tre$" "")
        multipart [{:name "name"       :content label}
                   {:name "newickData" :content f}]
        res       (client/post (tree-parser-url)
                               {:multipart        multipart
                                :throw-exceptions false})]
    (if (< 199 (:status res) 300)
      (tu/format-tree-url label (string/trim (:body res)))
      (tree-parser-error res))))

(defn- save-file
  "Saves the contents of an input stream to a file and returns the SHA1 hash of
   the file contents."
  [contents infile]
  (let [digest   (MessageDigest/getInstance "SHA1")
        hex-byte #(Integer/toHexString (bit-and 0xff %))]
    (copy (DigestInputStream. contents digest) infile)
    (apply str (map hex-byte (seq (.digest digest))))))

(defn- save-tree-metaurl
  "Saves the URL used to obtain the tree URLs in the AVUs for the file."
  [path metaurl]
  (try+
   (di/save-tree-metaurl path metaurl)
   (catch [:error_code ce/ERR_REQUEST_FAILED] {:keys [body]}
     (log/warn "unable to save the tree metaurl for" path "-"
               (cheshire/generate-string (cheshire/parse-string body) {:pretty true})))
   (catch Exception e
     (log/warn e "unable to save the tree metaurl for" path))))

(defn- get-existing-tree-urls
  "Obtains existing tree URLs for either a file stored in the iPlant data store
   or a SHA1 hash obtained from the contents of a file."
  ([sha1]
     (log/debug "searching for existing tree URLs for SHA1 hash" sha1)
     (get-tree-urls sha1))
  ([user path]
     (log/debug "searching for existing tree URLs for user" user "and path" path)
     (when-let [metaurl (di/get-tree-metaurl user path)]
       (log/debug "metaurl for path" path "is" metaurl)
       (let [retval (get-tree-urls (ft/basename metaurl))]
         (log/debug "Return value of get-tree-urls is" retval)
         retval)))
  ([sha1 user path]
     (log/debug "searching for existing tree URLs for SHA1 hash" sha1)
     (let [metaurl (metaurl-for sha1)]
       (when-let [urls (get-tree-urls sha1)]
         (save-tree-metaurl path metaurl)
         urls))))

(defn- save-tree-file
  "Saves a tree file in the local file system."
  [dir index ^Phylogeny tree]
  (let [^PhylogenyWriter writer    (PhylogenyWriter/createPhylogenyWriter)
                         tree-name (.getName tree)
                         file-name (if (string/blank? tree-name)
                                     (str "tree_" index ".tre")
                                     (str tree-name ".tre"))
                         out-file  (file dir file-name)]
    (.toNewHampshire writer tree false true out-file)
    out-file))

(defn- extract-trees-from-other
  "Extracts trees from all supported formats except for NeXML."
  [dir ^File infile]
  (let [parser (ParserUtils/createParserDependingFileContents infile false)
        trees  (seq (PhylogenyMethods/readPhylogenies parser infile))]
    (mapv (partial save-tree-file dir) (range) trees)))

(defn- extract-trees
  "Extracts trees from a tree file."
  [dir infile]
  (if (is-nexml? infile)
    (extract-trees-from-nexml dir infile)
    (extract-trees-from-other dir infile)))

(defn- get-tree-viewer-urls
  "Obtains the tree viewer URLs for the contents of a tree file."
  [dir infile]
  (log/debug "getting new tree URLs")
  (try
    (mapv get-tree-viewer-url (extract-trees dir infile))
    (catch PhylogenyParserException e
      (log/warn e "assuming that the given file contains no trees")
      [])))

(defn- build-response-map
  "Builds the map to use when formatting the response body."
  [urls]
  (when urls (assoc (tu/format-tree-urls urls) :action "tree_manifest")))

(defn- get-and-save-tree-viewer-urls
  "Gets the tree-viewer URLs for a file and stores them via the tree-urls service.  If the username and path to the
   file are also provided then a path containing the SHA1 will also be storeed in the AVUs for the file."
  ([dir infile sha1]
     (let [urls (get-tree-viewer-urls dir infile)]
       (set-tree-urls sha1 urls)
       (build-response-map urls)))
  ([path user dir infile sha1]
     (let [urls    (get-tree-viewer-urls dir infile)
           metaurl (metaurl-for sha1)]
       (set-tree-urls sha1 urls)
       (save-tree-metaurl path metaurl)
       (build-response-map urls))))

(defn tree-urls-response
  "Formats the response for one of the tree viewer URL services."
  [resp]
  (if (seq? resp)
    (success-response {:urls resp})
    (success-response {:urls (:tree-urls resp)})))

(defn tree-viewer-urls-for
  "Obtains the tree viewer URLs for a request body."
  [body {:keys [refresh]}]
  (log/info "getting tree viewer URLs for a request body")
  (with-temp-dir-in dir (file "/tmp") "tv" temp-dir-creation-failure
    (let [infile (file dir "data.txt")
          sha1   (save-file body infile)]
      (tree-urls-response
       (if-let [tree-urls (when-not refresh (get-existing-tree-urls sha1))]
         (do (log/debug "found existing tree URLs for" sha1)
             (build-response-map tree-urls))
         (do (log/debug "generating new URLs for" sha1)
             (get-and-save-tree-viewer-urls dir infile sha1)))))))

(defn tree-viewer-urls
  "Obtains the tree viewer URLs for a tree file in iRODS."
  ([path]
     (tree-viewer-urls path (:shortUsername current-user) {}))
  ([path user {:keys [refresh]}]
     (log/debug "obtaining tree URLs for user" user "and path" path)
     (tree-urls-response
      (if-let [existing-urls (and (not refresh) (get-existing-tree-urls user path))]
        (build-response-map existing-urls)
        (with-temp-dir-in dir (file "/tmp") "tv" temp-dir-creation-failure
          (let [infile (file dir "data.txt")
                body   (:body (fileio/download user path))
                sha1   (save-file body infile)]
            (or (and (not refresh) (get-existing-tree-urls sha1 user path))
                (get-and-save-tree-viewer-urls path user dir infile sha1))))))))
