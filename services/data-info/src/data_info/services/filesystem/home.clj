(ns data-info.services.filesystem.home
  (:use [clojure-commons.validators]
        [data-info.services.filesystem.common-paths]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists?]]
        [clj-jargon.item-ops :only [mkdirs]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.util.config :as cfg]
            [data-info.services.filesystem.icat :as icat]
            [data-info.services.filesystem.validators :as validators]))

(defn- user-home-path
  [staging-dir user set-owner?]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [user-home (ft/path-join staging-dir user)]
      (if (not (exists? cm user-home))
        (mkdirs cm user-home))
      {:id   (str "/root" user-home)
       :path user-home})))

(defn do-homedir
  [{user :user}]
  (user-home-path (cfg/irods-home) user false))

(with-pre-hook! #'do-homedir
  (fn [params]
    (log-call "do-homedir" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-homedir (log-func "do-homedir"))
