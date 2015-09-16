(ns data-info.services.users
  (:use [clj-jargon.init :only [with-jargon]])
  (:require [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.permissions :as perm]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.validators :as validators]))

(defn- filtered-user-perms
  [cm user abspath]
  (let [filtered-users (set (conj (cfg/perms-filter) user (cfg/irods-user)))]
    (filter
     #(not (contains? filtered-users (:user %1)))
     (perm/list-user-perm cm abspath))))


(defn- list-perm
  [cm user abspath]
  {:path abspath
   :user-permissions (filtered-user-perms cm user abspath)})

(defn- list-perms
  [user abspaths]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/all-paths-exist cm abspaths)
    (validators/user-owns-paths cm user abspaths)
    (mapv (partial list-perm cm user) abspaths)))

(defn do-user-permissions
  [{user :user} {paths :paths}]
  {:paths (list-perms user paths)})

(with-pre-hook! #'do-user-permissions
  (fn [params body]
    (dul/log-call "do-user-permissions" params body)
    (validators/validate-num-paths (:paths body))))

(with-post-hook! #'do-user-permissions (dul/log-func "do-user-permissions"))
