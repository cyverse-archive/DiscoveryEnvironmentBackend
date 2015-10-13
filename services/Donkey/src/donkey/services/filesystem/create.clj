(ns donkey.services.filesystem.create
  (:use [clj-jargon.init :only [with-jargon]]
        [clj-jargon.permissions :only [set-owner]])
  (:require [clojure.tools.logging :as log]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [donkey.services.filesystem.validators :as validators]
            [donkey.services.filesystem.icat :as cfg]))

(defn ensure-created
  "If a folder doesn't exist, it creates the folder and makes the given user an owner of it.

   Parameters:
     user - the username of the user to become an owner of the new folder
     dir  - the absolute path to the folder"
  [^String user ^String dir]
  (with-jargon (cfg/jargon-cfg) [cm]
    (when-not (item/exists? cm dir)
      (validators/user-exists cm user)
      (log/info "creating" dir)
      (ops/mkdirs cm dir)
      (set-owner cm dir user))))
