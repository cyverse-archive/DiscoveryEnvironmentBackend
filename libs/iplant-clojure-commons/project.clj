(defproject org.iplantc/clojure-commons "5.0.0"
  :description "Common Utilities for Clojure Projects"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [cheshire "5.3.1"]
                 [clj-http "1.0.0"]
                 [clj-time "0.8.0"]
                 [com.cemerick/url "0.1.1"]
                 [commons-configuration/commons-configuration "1.10"]  ; provides org.apache.commons.configuration
                 [me.raynes/fs "1.4.6"]
                 [medley "0.5.5"]
                 [slingshot "0.10.3"]
                 [trptcolin/versioneer "0.2.0"]]
  :profiles {:test {:resource-paths ["resources" "test-resources"]}})
