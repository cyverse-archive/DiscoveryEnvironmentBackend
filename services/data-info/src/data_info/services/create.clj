(ns data-info.services.create
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.permissions :only [set-owner]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.validators :as validators]
            [data-info.util.paths :as paths]
            [data-info.services.stat :as stat]))


(defn create
  "Creates a directory at path on behalf of a user. The user
   becomes the owner of the new directory."
  [user path]
  (log/debug (str "create " user " " path))
  (with-jargon (cfg/jargon-cfg) [cm]
    (let [fixed-path (ft/rm-last-slash path)
          path-stack (take-while (complement nil?) (iterate ft/dirname fixed-path))
          [existing-paths paths-to-mk] ((juxt filter remove) #(item/exists? cm %) path-stack)
          target-dir (first existing-paths)]
      (log/spy existing-paths)
      (log/spy paths-to-mk)
      (when-not target-dir
        (throw+ {:error_code ERR_DOES_NOT_EXIST
                 :path (last paths-to-mk)}))
      (when-not (validators/good-string? fixed-path)
        (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
                 :path path}))
      (validators/user-exists cm user)
      (validators/path-writeable cm user target-dir)
      (validators/path-not-exists cm fixed-path)
      (ops/mkdirs cm fixed-path)
      (doseq [new-path paths-to-mk]
        (set-owner cm new-path user))
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
