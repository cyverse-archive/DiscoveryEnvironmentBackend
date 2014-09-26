(ns donkey.services.filesystem.anon
  (:require [clojure.tools.logging :as log]
            [clj-jargon.init :as init]
            [donkey.util.config :as cfg])
  (:import [org.irods.jargon.core.exception
            DataNotFoundException
            DuplicateDataException]
           [org.irods.jargon.core.pub.domain User]
           [org.irods.jargon.core.protovalues UserTypeEnum]))


(def ^:private anon-user "anonymous")


(defn- anon-user?
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
  "Creates the anonymous user, but only if it doesn't already exist."
  []
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (try
      (if-not (anon-user? cm)
        (let [user (user-obj anon-user (:zone cm))]
          (.addUser (:userAO cm) user)
          (log/warn "Added the" anon-user "user. The zone for it is" (:zone cm) ".")))
      (catch DuplicateDataException e
        (log/warn "Attempted to create the" anon-user "user, but it already exists.")))))
