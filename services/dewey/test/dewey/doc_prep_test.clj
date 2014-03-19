(ns dewey.doc-prep-test
  (:use midje.sweet
        dewey.doc-prep)
  (:import [java.util Date]
           [org.irods.jargon.core.protovalues FilePermissionEnum
                                              UserTypeEnum]
           [org.irods.jargon.core.pub.domain UserFilePermission]
           [org.irods.jargon.core.query CollectionAndDataObjectListingEntry]))


(defn- mk-acl
  []
  [(UserFilePermission. "name1" "name1" FilePermissionEnum/OWN UserTypeEnum/RODS_USER "zone1")
   (UserFilePermission. "name2" "name2" FilePermissionEnum/WRITE UserTypeEnum/RODS_USER "zone2")
   (UserFilePermission. "name3" "name3" FilePermissionEnum/READ UserTypeEnum/RODS_USER "zone3")
   (UserFilePermission. "name4" "name4" FilePermissionEnum/NULL UserTypeEnum/RODS_USER "zone4")])


(facts "about `format-acl`"
  (fact "Formats permissions correctly."
    (set (format-acl (mk-acl))) => #{{:permission :own   :user "name1#zone1"}
                                     {:permission :write :user "name2#zone2"}
                                     {:permission :read  :user "name3#zone3"}}))

(facts "about `format-time`"
  (fact "works for a java.util.Date object"
    (format-time (Date. 1386180216000)) => "2013-12-04T18:03:36.000")
  (fact "works for a string containing a posix time in milliseconds"
    (format-time "1386180216000") => "2013-12-04T18:03:36.000"))
