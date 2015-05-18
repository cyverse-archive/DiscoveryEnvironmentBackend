(defproject org.iplantc/service-logging "5.0.0"
  :description "Common Logging Utilities for Clojure Projects"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.apache.logging.log4j/log4j-api "2.2"]
                 [org.apache.logging.log4j/log4j-core "2.2"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.2"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [com.fasterxml.jackson.core/jackson-core "2.5.1"]
                 [com.fasterxml.jackson.core/jackson-databind "2.5.1"]
                 [com.fasterxml.jackson.core/jackson-annotations "2.5.1"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.5.1"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.5.1"]])
