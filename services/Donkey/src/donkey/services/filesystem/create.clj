(ns donkey.services.filesystem.create
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.util.config]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops :only [mkdir]]
        [clj-jargon.permissions :only [set-owner collection-perm-map]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.services.filesystem.stat :as stat]
            [donkey.services.filesystem.validators :as validators]))

(defn create
  "Creates a directory at path on behalf of a user. The user
   becomes the owner of the new directory."
  [user path]
  (log/debug (str "create " user " " path))
  (with-jargon (jargon-cfg) [cm]
    (let [fixed-path (ft/rm-last-slash path)]
      (when-not (good-string? fixed-path)
        (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
                 :path path}))
      (validators/user-exists cm user)
      (validators/path-writeable cm user (ft/dirname fixed-path))
      (validators/path-not-exists cm fixed-path)
      (mkdir cm fixed-path)
      (set-owner cm fixed-path user)
      (stat/path-stat cm user fixed-path))))

(defn do-create
  "Entrypoint for the API that calls (create)."
  [{user :user} {path :path}]
  (create user path))

(with-pre-hook! #'do-create
  (fn [params body]
    (log-call "do-create" params body)
    (validate-map params {:user string?})
    (validate-map body {:path string?})
    (log/info "Body: " body)
    (when (super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED :user (:user params)}))))

(with-post-hook! #'do-create (log-func "do-create"))
