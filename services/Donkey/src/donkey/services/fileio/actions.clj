(ns donkey.services.fileio.actions
  (:use [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info]
        [clj-jargon.item-ops]
        [clj-jargon.metadata]
        [clj-jargon.users :only [user-exists?]]
        [clj-jargon.permissions]
        [clojure-commons.error-codes]
        [donkey.util.service :only [success-response]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cemerick.url :as url]
            [cheshire.core :as json]
            [clojure-commons.file-utils :as ft]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clj-http.client :as client]
            [ring.util.response :as rsp-utils]
            [donkey.clients.data-info :as data]
            [donkey.util.config :as cfg]
            [donkey.services.fileio.config :as jargon]
            [donkey.services.filesystem.validators :as validators]
            [donkey.services.metadata.apps :as apps]))


(defn set-meta
  [path attr value unit]
  (with-jargon (jargon/jargon-cfg) [cm]
    (set-metadata cm path attr value unit)))

(defn copy-metadata
  "Copies AVUs from src and applies them to dest."
  [cm src dest]
  (doseq [m (get-metadata cm src)]
    (set-metadata cm dest (:attr m) (:value m) (:unit m))))

(defn save
  [cm istream user dest-path]
  (log/info "In save function for " user dest-path)
  (let [ddir (ft/dirname dest-path)]
    (when-not (exists? cm ddir)
      (mkdirs cm ddir))

    (copy-stream cm istream user dest-path)
    (log/info "save function after copy.")
    dest-path))

(defn store
  [cm istream user dest-path]
  (log/info "In store function for " user dest-path)
  (let [ddir (ft/dirname dest-path)]
    (when-not (is-writeable? cm user ddir)
      (log/error (str "Directory " ddir " is not writeable by " user))
      (throw+ {:error_code ERR_NOT_WRITEABLE
               :path ddir} )))

  (save cm istream user dest-path)
  dest-path)


(defn- get-istream
  [user file-path]
  (with-jargon (jargon/jargon-cfg) [cm]
    (when-not (user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER
               :user       user}))

    (when-not (exists? cm file-path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST
               :path       file-path}))

    (when-not (is-readable? cm user file-path)
      (throw+ {:error_code ERR_NOT_READABLE
               :user       user
               :path       file-path}))

    (if (= (file-size cm file-path) 0)
      ""
      (input-stream cm file-path))))

(defn- new-filename
  [tmp-path]
  (string/join "." (drop-last (string/split (ft/basename tmp-path) #"\."))))

(defn upload
  [user tmp-path fpath]
  (log/info "In upload for " user tmp-path fpath)
  (let [final-path (ft/rm-last-slash fpath)]
    (with-jargon (jargon/jargon-cfg) [cm]
      (when-not (user-exists? cm user)
        (throw+ {:error_code ERR_NOT_A_USER
                 :user user}))

      (when-not (exists? cm final-path)
        (throw+ {:error_code ERR_DOES_NOT_EXIST
                 :id final-path}))

      (when-not (is-writeable? cm user final-path)
        (throw+ {:error_code ERR_NOT_WRITEABLE
                 :id final-path}))

      (let [new-fname (new-filename tmp-path)
            new-path  (ft/path-join final-path new-fname)]
        (if (exists? cm new-path) (delete cm new-path))
        (move cm tmp-path new-path
          :user               user
          :admin-users        (cfg/irods-admins)
          :skip-source-perms? true)
        (set-owner cm new-path user)
        (success-response {:file (data/path-stat user new-path)})))))


(defn url-encoded?
  [string-to-check]
  (re-seq #"\%[A-Fa-f0-9]{2}" string-to-check))

(defn url-encode-path
  [path-to-encode]
  (string/join "/"
   (mapv
    #(if-not (url-encoded? %1)
       (url/url-encode %1)
       %1)
    (string/split path-to-encode #"\/"))))

(defn url-encode-url
  [url-to-encode]
  (let [full-url (url/url url-to-encode)]
    (str (assoc full-url :path (url-encode-path (:path full-url))))))

(defn urlimport
  "Submits a URL import job for execution.

   Parameters:
     user - string containing the username of the user that requested the import.
     address - string containing the URL of the file to be imported.
     filename - the filename of the file being imported.
     dest-path - irods path indicating the directory the file should go in."
  [user address filename dest-path]
  (let [filename  (if (url-encoded? filename) (url/url-decode filename) filename)
        dest-path (ft/rm-last-slash dest-path)]
    (with-jargon (jargon/jargon-cfg) [cm]
      (validators/user-exists cm user)
      (validators/path-writeable cm user dest-path)
      (validators/path-not-exists cm (ft/path-join dest-path filename)))
    (apps/url-import address filename dest-path)
    (success-response
     {:msg   "Upload scheduled."
      :url   address
      :label filename
      :dest  dest-path})))

(defn download
  "Returns a response map filled out with info that lets the client download
   a file."
  [user file-path]
  (log/debug "In download.")
  (let [istream (get-istream user file-path)]
    (-> {:status 200
         :body istream}
      (rsp-utils/header
        "Content-Disposition"
        (str "attachment; filename=\"" (ft/basename file-path) "\""))
      (rsp-utils/header
       "Content-Type"
       "application/octet-stream")
      success-response)))
