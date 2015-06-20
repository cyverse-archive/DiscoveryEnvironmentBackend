(defproject org.iplantc/clojure-commons "5.0.0"
  :description "Common Utilities for Clojure Projects"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[joda-time "2.4"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-http "1.0.0"]
                 [clj-http-fake "0.7.8"]
                 [clj-time "0.8.0"]
                 [com.cemerick/url "0.1.1"]
                 [org.apache.logging.log4j/log4j-api "2.2"]
                 [org.apache.logging.log4j/log4j-core "2.2"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.2"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [com.fasterxml.jackson.core/jackson-core "2.5.1"]
                 [com.fasterxml.jackson.core/jackson-databind "2.5.1"]
                 [com.fasterxml.jackson.core/jackson-annotations "2.5.1"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.5.1"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.5.1"]
                 [me.raynes/fs "1.4.6"]
                 [medley "0.5.5"]
                 [slingshot "0.10.3"]
                 [org.mongodb/mongo-java-driver "2.12.3"]
                 [commons-configuration/commons-configuration "1.10"]
                 [trptcolin/versioneer "0.2.0"]
                 [cheshire "5.3.1"]]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]]
  :profiles {:test {:resource-paths ["resources" "test-resources"]}})
