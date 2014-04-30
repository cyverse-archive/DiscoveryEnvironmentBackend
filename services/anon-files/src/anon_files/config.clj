(ns anon-files.config
  (:require [bouncer [core :as b] [validators :as v]]
            [common-cfg.cfg :as cfg]))

(dosync
 (ref-set
  cfg/validators
   {:port           [v/required v/number]
    :irods-host     [v/required cfg/stringv]
    :irods-port     [v/required cfg/stringv]
    :irods-zone     [v/required cfg/stringv]
    :irods-home     [v/required cfg/stringv]
    :irods-user     [v/required cfg/stringv]
    :irods-password [v/required cfg/stringv]
    :anon-user      [v/required cfg/stringv]
    :log-file       cfg/stringv
    :log-size       v/number
    :log-backlog    v/number
    :log-level      cfg/keywordv})

 (ref-set
  cfg/defaults
  {:log-level   :info
   :log-size    (* 100 1024 1024)
   :log-backlog 10})

 (ref-set
  cfg/filters
  #{:irods-password}))
