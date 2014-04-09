(ns donkey.services.filesystem.anonymous
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.util.config]
        [donkey.services.filesystem.validators]
        [donkey.services.filesystem.common-paths])
  (:require [clojure.tools.logging :as log]
            [clj-jargon.init :as init]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.item-info :as info]
            [clj-jargon.permissions :as perms]
            [dire.core :refer [with-pre-hook! with-post-hook!]])
  (:import [org.irods.jargon.core.exception
            DataNotFoundException
            DuplicateDataException]
           [org.irods.jargon.core.pub.domain User]
           [org.irods.jargon.core.protovalues UserTypeEnum]))
;;; TODO: The permissions for the anon user need to go up to the user's home
;;; directory, otherwise the i-command access for anonymous won't work.



(def anon-user "anonymous")

(defn anon-user?
  "Returns boolean indicating whether the anonymous user exists."
  [cm]
  (try
    (not (nil? (.findByName (:userAO cm) anon-user)))
    (catch DataNotFoundException e
      false)))

(defn- user-obj
  [username zone]
  (doto (User.)
    (.setName anon-user)
    (.setZone zone)
    (.setUserType (UserTypeEnum/findTypeByString "rodsuser"))))

(defn create-anon-user
  "Creates the anonymous user."
  [cm]
  (try
    (let [user (user-obj anon-user (:zone cm))]
      (.addUser (:userAO cm) user)
      (log/warn "Added the" anon-user "user. The zone for it is" (:zone cm) "."))
    (catch DuplicateDataException e
      (log/warn "Attempted to create the" anon-user "user, but it already exists."))))

(defn share-with-anon
  "Gives read permissions on the iRODS file/folder to the anonymous user."
  [user filepaths]
  (init/with-jargon (jargon-cfg) [cm]
    (user-exists cm user)
    (all-paths-exist cm filepaths)
    (user-owns-paths cm user filepaths)
    (when-not (anon-user? cm)
      (log/warn "The" anon-user "does not exist.")
      (create-anon-user cm))
    (doseq [filepath filepaths]
      (when-not (perms/is-readable? cm anon-user filepath)
        (log/warn "Giving" anon-user "read permissions on" filepath)
        (perms/set-permission cm anon-user filepath :read)))))

(defn do-share-with-anon
  [params body]
  (log-call "do-share-with-anon" params body)
  (validate-map params {:user string?})
  (validate-map body {:paths sequential?})
  (share-with-anon (:user params) (:paths body)))

(with-post-hook! #'do-share-with-anon (log-func "do-share-with-anon"))
