(ns anon-files.config
  (:require [bouncer [core :as b] [validators :as v]]
            [common-cfg.cfg :as cfg]))

(dosync
 (ref-set
  cfg/validators
   {:port           [cfg/intablev]
    :irods-host     [v/required cfg/stringv]
    :irods-port     [cfg/stringv]
    :irods-zone     [v/required cfg/stringv]
    :irods-home     [v/required cfg/stringv]
    :irods-user     [v/required cfg/stringv]
    :irods-password [v/required cfg/stringv]
    :anon-user      [cfg/stringv]})

 (ref-set
   cfg/defaults
   {:port       "60000"
    :anon-user  "anonymous"
    :irods-port "1247"})

 (ref-set
  cfg/filters
  #{:irods-password}))
