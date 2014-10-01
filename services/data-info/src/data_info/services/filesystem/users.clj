(ns data-info.services.filesystem.users
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [data-info.services.filesystem.common-paths]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [quota]]
        [clj-jargon.users])
  (:require [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.permissions :as perm]
            [data-info.util.config :as cfg]
            [data-info.services.filesystem.icat :as icat]
            [data-info.services.filesystem.validators :as validators]))


(defn list-user-groups
  [user]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (user-groups cm user)))


(defn- filtered-user-perms
  [cm user abspath]
  (let [filtered-users (set (conj (cfg/perms-filter) user (cfg/irods-user)))]
    (filter #(not (contains? filtered-users (:user %1)))
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

(defn- get-quota
  [user]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (quota cm user)))

(defn do-groups
  [{user :user}]
  {:groups (list-user-groups user)})

(with-pre-hook! #'do-groups
  (fn [params]
    (log-call "do-groups" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-groups (log-func "do-groups"))

(defn do-quota
  [{user :user}]
  {:quotas (get-quota user)})

(with-pre-hook! #'do-quota
  (fn [params]
    (log-call "do-quota" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-quota (log-func "do-quota"))

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
