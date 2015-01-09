(defproject job-preserver "4.1.1"
  :description "Migration to move job data that was skipped in previous migrations."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :dependencies [[cheshire "5.3.1"]
                 [clj-time "0.8.0"]
                 [com.novemberain/monger "1.5.0"]
                 [korma "0.3.2"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.iplantc/kameleon "4.1.1"]]
  :uberjar-name "job-preserver.jar"
  :aot [job-preserver.core]
  :main job-preserver.core)
