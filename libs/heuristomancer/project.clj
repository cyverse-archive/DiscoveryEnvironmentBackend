(defproject org.iplantc/heuristomancer "5.0.0"
  :description "Clojure library for attempting to guess file types."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :profiles {:dev {:resource-paths ["test-data"]}}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [instaparse "1.4.1"]]
  :plugins [[org.iplantc/lein-iplant-cmdtar "5.0.0"]]
  :aot [heuristomancer.core]
  :main heuristomancer.core)
