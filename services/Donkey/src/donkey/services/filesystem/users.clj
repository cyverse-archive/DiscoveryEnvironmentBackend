(ns donkey.services.filesystem.users
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.users]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.permissions :as perm]
            [donkey.util.config :as cfg]
            [donkey.services.filesystem.icat :as icat]
            [donkey.services.filesystem.validators :as validators]))

(defn list-user-groups
  [user]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (user-groups cm user)))

(defn- filtered-user-perms
  [cm user abspath]
  (let [filtered-users (set (conj (cfg/fs-perms-filter) user (cfg/irods-user)))]
    (filter
     #(not (contains? filtered-users (:user %1)))
     (perm/list-user-perm cm abspath))))


(defn- list-perm
  [cm user abspath]
  {:path abspath
   :user-permissions (filtered-user-perms cm user abspath)})

(defn- list-perms
  [user abspaths]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/all-paths-exist cm abspaths)
    (validators/user-owns-paths cm user abspaths)
    (mapv (partial list-perm cm user) abspaths)))

(defn do-user-permissions
  [{user :user} {paths :paths}]
  {:paths (list-perms user paths)})

(with-pre-hook! #'do-user-permissions
  (fn [params body]
    (log-call "do-user-permissions" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential?})
    (validators/validate-num-paths (:paths body))))

(with-post-hook! #'do-user-permissions (log-func "do-user-permissions"))


(defn ^Boolean owns?
  "Indicates if a file or folder is owned by a given user.

   Parameters:
     user       - the username of the user
     entry-path - The absolute path to the file or folder

   Returns:
     It returns true if the user own the entry, otherwise false."
  [^String user ^String entry-path]
  (with-jargon (icat/jargon-cfg) [cm]
    (perm/owns? cm user entry-path)))
