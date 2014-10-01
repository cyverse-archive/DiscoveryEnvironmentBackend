(ns data-info.services.filesystem.preview
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [data-info.services.filesystem.common-paths]
        [data-info.services.filesystem.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [file-size]]
        [clj-jargon.item-ops :only [read-file]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.util.config :as cfg]
            [data-info.services.filesystem.icat :as icat]
            [data-info.services.filesystem.validators :as validators]))


(defn- preview-buffer
  [cm path size]
  (let [realsize (file-size cm path)
        buffsize (if (<= realsize size) realsize size)
        buff     (char-array buffsize)]
    (read-file cm path buff)
    (.append (StringBuilder.) buff)))

(defn- gen-preview
  [cm path size]
  (if (zero? (file-size cm path))
    ""
    (str (preview-buffer cm path size))))

(defn- preview
  "Grabs a preview of a file in iRODS.

   Parameters:
     user - The username of the user requesting the preview.
     path - The path to the file in iRODS that will be previewed.
     size - The size (in bytes) of the preview to be created."
  [user path size]
  (let [path (ft/rm-last-slash path)]
    (with-jargon (icat/jargon-cfg) [cm]
      (log/debug (str "preview " user " " path " " size))
      (validators/user-exists cm user)
      (validators/path-exists cm path)
      (validators/path-readable cm user path)
      (validators/path-is-file cm path)
      (gen-preview cm path size))))


(defn do-preview
  [{user :user path :path}]
  {:preview (preview user path (cfg/preview-size))})


(with-pre-hook! #'do-preview
  (fn [params]
    (log-call "do-preview" params)
    (validate-map params {:user string? :path string?})
    (when (super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED
               :user (:user params)
               :path (:path params)}))))

(with-post-hook! #'do-preview (log-func "do-preview"))
