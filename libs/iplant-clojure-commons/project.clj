(defproject org.iplantc/clojure-commons "5.0.0"
  :description "Common Utilities for Clojure Projects"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [buddy/buddy-sign "0.7.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [metosin/compojure-api "0.23.1"]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [clj-time "0.11.0"]
                 [com.cemerick/url "0.1.1"]
                 [commons-configuration "1.10"    ; provides org.apache.commons.configuration
                  :exclusions [commons-logging]]
                 [me.raynes/fs "1.4.6"]
                 [medley "0.7.0"]
                 [metosin/compojure-api "0.23.1"]
                 [slingshot "0.12.2"]
                 [trptcolin/versioneer "0.2.0"]
                 [org.iplantc/service-logging "5.0.0"]]
  :profiles {:test {:resource-paths ["resources" "test-resources"]}})
