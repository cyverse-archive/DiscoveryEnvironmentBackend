(defproject org.iplantc/monkey "4.2.0"
  :description "A metadata database crawler. It synchronizes the tag documents in the search data
                index with the tag information inthe metadata database.  🐒"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :aot [monkey.index monkey.tags monkey.core]
  :main monkey.core
  :uberjar-name "monkey-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [postgresql "9.1-901-1.jdbc4"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.clojure/tools.logging "0.3.0"]
                 [clojurewerkz/elastisch "2.0.0"]
                 [com.novemberain/langohr "2.11.0"]
                 [me.raynes/fs "1.4.6"]
                 [slingshot "0.10.3"]
                 [org.iplantc/clojure-commons "4.2.0"]
                 [org.iplantc/common-cli "4.2.0"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :plugins [[org.iplantc/lein-iplant-rpm "4.2.0"]]
  :iplant-rpm {:summary      "monkey"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path  "conf/main"})
