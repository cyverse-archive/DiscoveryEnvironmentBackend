(ns data-info.services.filetypes
  (:use [clj-jargon.init :only [with-jargon]]
        [clj-jargon.metadata])
  (:require [heuristomancer.core :as hm]
            [clojure.string :as string]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.validators :as validators]
            [clojure-commons.file-utils :as ft]
            [data-info.services.uuids :as uuids]
            [dire.core :refer [with-pre-hook! with-post-hook!]]))

(def script-types (sort (hm/supported-formats)))

(defn- get-type-list
  []
  {:types script-types})

(defn- add-type
  "Adds the type to a file in iRODS at path for the specified user."
  [user path type]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/path-exists cm path)
    (validators/user-exists cm user)
    (validators/user-owns-path cm user path)
    (validators/path-is-file cm path)

    (set-metadata cm path (cfg/type-detect-type-attribute) type "")
    {:path path
     :type type
     :user user}))

(defn- unset-types
  "Removes all info-type associations from a path."
  [user path]

  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/path-exists cm path)
    (validators/user-exists cm user)
    (validators/user-owns-path cm user path)

    (if (attribute? cm path (cfg/type-detect-type-attribute))
      (delete-metadata cm path (cfg/type-detect-type-attribute)))
    {:path path
     :type ""
     :user user}))

(defn- add-type-path
  [user path type]
  (if-not (string/blank? type)
    (add-type user path type)
    (unset-types user path)))

(defn do-add-type-uuid
  [{user :user} {type :type} data-id]
  (let [path (ft/rm-last-slash (:path (uuids/path-for-uuid user data-id)))]
    (add-type-path user path type)))

(with-pre-hook! #'do-add-type-uuid
  (fn [params body data-id]
    (dul/log-call "do-add-type-uuid" params body data-id)))

(with-post-hook! #'do-add-type-uuid (dul/log-func "do-add-type-uuid"))

(defn do-type-list
  []
  (get-type-list))

(with-pre-hook! #'do-type-list
  (fn []
    (dul/log-call "do-type-list")))

(with-post-hook! #'do-type-list (dul/log-func "do-type-list"))
