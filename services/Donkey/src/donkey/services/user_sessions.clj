(ns donkey.services.user-sessions
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes]
        [donkey.util.config]
        [donkey.util.service]
        [donkey.clients.user-sessions]
        [donkey.auth.user-attributes])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]))

(defn user-session
  ([]
     (let [user (:username current-user)]
       (log/debug "Getting user session for" user)
       (get-session user)))
  ([session]
     (let [user (:username current-user)]
       (log/debug "Setting user session for" user)
       (set-session user session))))

(defn remove-session
  []
  (let [user (:username current-user)]
    (log/debug "Deleting user session for" user)
    (delete-session user)))
