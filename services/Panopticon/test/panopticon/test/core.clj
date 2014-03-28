(ns panopticon.test.core
  (:use [panopticon.core] :reload)
  (:use [midje.sweet])
  (:require [clojure-commons.osm :as osm]
            [cheshire.core :as json]))

(fact
 (boolize "true") => true
 (boolize "false") => false)

(reset! props
        {"panopticon.osm.url" "http://iplantcollaborative.org"
         "panopticon.osm.collection" "collection"
         "panopticon.condor.condor-config" "/etc/condor/condor-config"
         "panopticon.condor.condor-q" "condor-q"
         "panopticon.condor.condor-history" "condor-history"
         "panopticon.app.num-instances" "1"
         "panopticon.app.partition-size" "10"})

(fact
 (osm-url) => "http://iplantcollaborative.org"
 (osm-coll) => "collection"
 (condor-config) => "/etc/condor/condor-config"
 (condor-q) => "condor-q"
 (condor-history) => "condor-history"
 (num-instances) => 1
 (part-size) => 10)

(def janky-classad
  {"JobStatus" "1"})

(fact
 (job-status janky-classad) => "Idle")

(with-redefs
  [osm/query (fn [cl query] (json/encode {:objects [{:state.status "Idle"}]}))]
  (fact
   (running-jobs) => [{:state.status "Idle"}]))


