(ns data-info.services.create
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [data-info.services.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.permissions :only [set-owner collection-perm-map]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.validations :as valid]
            [data-info.util.logging :as dul]
            [data-info.services.common-paths :as paths]
            [data-info.services.icat :as cfg]
            [data-info.services.stat :as stat]
            [data-info.services.validators :as validators]))

(defn create
  "Creates a directory at path on behalf of a user. The user
   becomes the owner of the new directory."
  [user path]
  (log/debug (str "create " user " " path))
  (with-jargon (cfg/jargon-cfg) [cm]
    (let [fixed-path (ft/rm-last-slash path)]
      (when-not (valid/good-string? fixed-path)
        (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
                 :path path}))
      (validators/user-exists cm user)
      (validators/path-writeable cm user (ft/dirname fixed-path))
      (validators/path-not-exists cm fixed-path)
      (ops/mkdir cm fixed-path)
      (set-owner cm fixed-path user)
      (stat/path-stat cm user fixed-path))))

(defn do-create
  "Entrypoint for the API that calls (create)."
  [{user :user} {path :path}]
  (create user path))

(with-pre-hook! #'do-create
  (fn [params body]
    (dul/log-call "do-create" params body)
    (validate-map params {:user string?})
    (validate-map body {:path string?})
    (log/info "Body: " body)
    (when (paths/super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED :user (:user params)}))))

(with-post-hook! #'do-create (dul/log-func "do-create"))


(defn ensure-created
  "If a folder doesn't exist, it creates the folder and makes the given user an owner of it.

   Parameters:
     user - the username of the user to become an owner of the new folder
     dir  - the absolute path to the folder"
  [^String user ^String dir]
  (with-jargon (cfg/jargon-cfg) [cm]
    (when-not (item/exists? cm dir)
      (log/info "creating" dir)
      (ops/mkdirs cm dir)
      (set-owner cm dir user))))
