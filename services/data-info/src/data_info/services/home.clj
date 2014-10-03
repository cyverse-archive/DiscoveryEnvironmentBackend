(ns data-info.services.home
  (:require [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :refer [exists?]]
            [clj-jargon.item-ops :refer [mkdirs]]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.services.common-paths :as log]
            [data-info.services.icat :as icat]
            [data-info.services.uuids :as uuid]
            [data-info.services.validators :as validators]))


(defn- user-home-path
  [staging-dir user]
  (let [user-home (ft/path-join staging-dir user)]
    (with-jargon (icat/jargon-cfg) [cm]
      (when-not (exists? cm user-home)
        (mkdirs cm user-home))
      {:id   (uuid/lookup-uuid cm user-home)
       :path user-home})))


(defn do-homedir
  [{user :user}]
  (user-home-path (cfg/irods-home) user))

(with-pre-hook! #'do-homedir
  (fn [params]
    (log/log-call "do-homedir" params)
    (cv/validate-map params {:user string?})
    (validators/user-exists (:user params))))

(with-post-hook! #'do-homedir (log/log-func "do-homedir"))
