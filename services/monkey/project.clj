(defproject org.iplantc/monkey "3.2.2"
  :description "A metadata database crawler. It synchronizes the tag documents in the search data
                index with the tag information inthe metadata database.  🐒"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :aot [monkey.core]
  :main monkey.core
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [me.raynes/fs "1.4.6"]
                 [slingshot "0.10.3"]
                 [org.iplantc/clojure-commons "3.2.2"]
                 [org.iplantc/common-cli "3.2.2"]])