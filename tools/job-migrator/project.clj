(defproject job-migrator "3.2.4"
  :description "Migration to move more job data into Postgres."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :dependencies [[cheshire "5.3.1"]
                 [clj-http "0.9.2"]
                 [com.cemerick/url "0.1.1"]
                 [korma "0.3.2"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.0"]
                 [org.iplantc/clojure-commons "3.2.4"]
                 [org.iplantc/mescal "3.2.4"]
                 [mvxcvi/clj-pgp "0.5.2"]
                 [postgresql "9.0-801.jdbc4"]
                 [slingshot "0.10.3"]]
  :uberjar-name "job-migrator.jar"
  :aot [job-migrator.core]
  :main job-migrator.core)
