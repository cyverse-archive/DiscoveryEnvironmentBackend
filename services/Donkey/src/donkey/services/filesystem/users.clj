(ns donkey.services.filesystem.users
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.users])
  (:require [clj-jargon.permissions :as perm]
            [donkey.services.filesystem.icat :as icat]
            [donkey.services.filesystem.validators :as validators]))

(defn list-user-groups
  [user]
  (with-jargon (icat/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (user-groups cm user)))


(defn ^Boolean owns?
  "Indicates if a file or folder is owned by a given user.

   Parameters:
     user       - the username of the user
     entry-path - The absolute path to the file or folder

   Returns:
     It returns true if the user own the entry, otherwise false."
  [^String user ^String entry-path]
  (with-jargon (icat/jargon-cfg) [cm]
    (perm/owns? cm user entry-path)))
