(ns donkey.services.filesystem.copy
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.util.config]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops]
        [clj-jargon.metadata :only [set-metadata]]
        [clj-jargon.permissions :only [set-owner]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.services.filesystem.validators :as validators]))

(defn- copy-path
  ([copy-map]
     (copy-path copy-map "ipc-de-copy-from"))

  ([{:keys [user from to]} copy-key]
     (with-jargon (jargon-cfg) [cm]
       (validators/user-exists cm user)
       (validators/all-paths-exist cm from)
       (validators/all-paths-readable cm user from)
       (validators/path-exists cm to)
       (validators/path-writeable cm user to)
       (validators/path-is-dir cm to)
       (validators/no-paths-exist cm (mapv #(ft/path-join to (ft/basename %)) from))

       (when (some true? (mapv #(= to %1) from))
         (throw+ {:error_code ERR_INVALID_COPY
                  :paths (filterv #(= to %1) from)}))

       (doseq [fr from]
         (let [metapath (ft/rm-last-slash (ft/path-join to (ft/basename fr)))]
           (copy cm fr to)
           (set-metadata cm metapath copy-key fr "")
           (set-owner cm to user)))

       {:sources from :dest to})))

(defn do-copy
  [{user :user} {paths :paths destination :destination}]
  (copy-path
    {:user user
     :from paths
     :to   destination}
    (fs-copy-attribute)))

(with-pre-hook! #'do-copy
  (fn [params body]
    (log-call "do-copy" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential? :destination string?})
    (validate-num-paths (:paths body))))

(with-post-hook! #'do-copy (log-func "do-copy"))
