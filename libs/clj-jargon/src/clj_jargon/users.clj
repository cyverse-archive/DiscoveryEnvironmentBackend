(ns clj-jargon.users
  (:use [clj-jargon.validations]
        [clj-jargon.gen-query])
  (:import [org.irods.jargon.core.exception DataNotFoundException]
           [org.irods.jargon.core.query RodsGenQueryEnum]
           [org.irods.jargon.core.pub UserGroupAO
                                      UserAO]
           [org.irods.jargon.core.pub.domain UserGroup]))


(defn username->id
  [cm user]
  (->> (execute-gen-query cm
        "select %s where %s = '%s'"
        [RodsGenQueryEnum/COL_USER_ID
         RodsGenQueryEnum/COL_USER_NAME
         user])
       (mapv result-row->vec)
       (first)
       (first)))

(defn user-groups
  "Returns a list of group names that the user is in."
  [{^UserGroupAO ug-ao :userGroupAO} user]
  (for [^UserGroup ug (.findUserGroupsForUser ug-ao user)]
    (.getUserGroupName ug)))

(defn user-exists?
  "Returns true if 'user' exists in iRODS."
  [{^UserAO user-ao :userAO} user]
  (try
    (do
      (.findByName user-ao user)
      true)
    (catch DataNotFoundException d false)))
