(defproject org.iplantc/clj-jargon "4.0.1"
  :description "Clojure API on top of iRODS's jargon-core."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.iplantc/clojure-commons "4.0.1"]
                 [org.slf4j/slf4j-api "1.7.5"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [org.irods.jargon/jargon-core "3.3.1.1"
                  :exclusions [[org.jglobus/JGlobus-Core]
                               [org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.irods.jargon/jargon-data-utils "3.3.1.1"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.irods.jargon.transfer/jargon-transfer-engine "3.3.1.1"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]
                               [org.irods.jargon.transfer/jargon-transfer-dao-spring]]]
                 [org.irods.jargon/jargon-security "3.3.1.1"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.irods.jargon/jargon-ticket "3.3.1.1"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [slingshot "0.10.3"]
                 [org.clojure/tools.logging "0.2.6"]]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]
                 ["renci.repository.releases"
                  {:url "http://ci-dev.renci.org/nexus/content/repositories/releases/"}]
                 ["renci.repository.snapshots"
                  {:url "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
