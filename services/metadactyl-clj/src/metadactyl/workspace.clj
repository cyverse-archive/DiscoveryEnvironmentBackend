(ns metadactyl.workspace
  (:use [korma.core]
        [kameleon.queries]
        [metadactyl.user :only [current-user]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as cc-errs]))

(defn get-workspace
  "Gets a workspace database entry for the given username or the current user."
  ([]
   (get-workspace (:username current-user)))
  ([username]
   (if-let [workspace (fetch-workspace-by-user-id (get-existing-user-id username))]
     workspace
     (throw+ {:code     cc-errs/ERR_NOT_FOUND,
              :username username,
              :message  "Workspace for user not found."}))))
