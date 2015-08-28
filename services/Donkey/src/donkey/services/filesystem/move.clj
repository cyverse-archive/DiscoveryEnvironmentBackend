(ns donkey.services.filesystem.move
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops :only [move-all]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-icat-direct.icat :as icat]
            [donkey.util.config :as cfg]
            [donkey.services.filesystem.directory :as directory]
            [donkey.services.filesystem.icat :as jargon]
            [donkey.services.filesystem.validators :as validators]))

(defn- source->dest
  [source-path dest-path]
  (ft/path-join dest-path (ft/basename source-path)))

(defn- move-paths
  "Moves directories listed in 'sources' into the directory listed in 'dest'. This
   works by calling move and passing it move-dir."
  [user sources dest]
  (with-jargon (jargon/jargon-cfg) [cm]
    (let [path-list  (conj sources dest)
          all-paths  (apply merge (mapv #(hash-map (source->dest %1 dest) %1) sources))
          dest-paths (keys all-paths)
          sources    (mapv ft/rm-last-slash sources)
          dest       (ft/rm-last-slash dest)]
      (validators/user-exists cm user)
      (validators/all-paths-exist cm sources)
      (validators/all-paths-exist cm [dest])
      (validators/path-is-dir cm dest)
      (validators/user-owns-paths cm user sources)
      (validators/path-writeable cm user dest)
      (validators/no-paths-exist cm dest-paths)
      (move-all cm sources dest :user user :admin-users (cfg/irods-admins))
      {:sources sources :dest dest})))

(defn do-move
  [{user :user} {sources :sources dest :dest}]
  (move-paths user sources dest))

(with-pre-hook! #'do-move
  (fn [params body]
    (log-call "do-move" params body)
    (validate-map params {:user string?})
    (validate-map body {:sources sequential? :dest string?})
    (log/info "Body: " (json/encode body))
    (when (super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED
               :user (:user params)}))
    (validators/validate-num-paths-under-paths (:user params) (:sources body))))

(with-post-hook! #'do-move (log-func "do-move"))
