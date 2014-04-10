(ns anon-files.serve
  (:use [ring.util.response]
        [anon-files.config])
  (:require [clj-jargon.init :as init]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.item-info :as info]
            [clj-jargon.permissions :as perms]))

(def jargon-cfg
  (memoize
   (fn []
     (init/init
      (irods-host)
      (irods-port)
      (irods-user)
      (irods-pass)
      (irods-home)
      (irods-zone)
      ""))))

(defn serve
  [filepath]
  (init/with-jargon (jargon-cfg) [cm]
    (cond
     (not (info/exists? cm filepath))
     (do (println "[anon-files]" filepath "does not exist.")
       (not-found "Not found."))

     (not (info/is-file? cm filepath))
     (do (println "[anon-files]" filepath "is not a file.")
       (-> (response "Not a file.") (status 403)))

     (not (perms/is-readable? cm (irods-user) filepath))
     (do (println "[anon-files]" filepath "is not readable.")
       (-> (response "Insufficient privileges.") (status 403)))

     :else
     (ops/input-stream cm filepath))))
