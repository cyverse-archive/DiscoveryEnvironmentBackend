(defproject org.iplantc/clj-jargon "4.1.5"
  :description "Clojure API on top of iRODS's jargon-core."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.iplantc/clojure-commons "4.1.5"]
                 [org.slf4j/slf4j-api "1.7.10"]
                 [org.slf4j/slf4j-log4j12 "1.7.10"]
                 [org.irods.jargon/jargon-core "4.0.2-RELEASE"
                  :exclusions [[org.jglobus/JGlobus-Core]
                               [org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.irods.jargon/jargon-data-utils "4.0.2-RELEASE"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [org.irods.jargon/jargon-ticket "4.0.2-RELEASE"
                  :exclusions [[org.slf4j/slf4j-api]
                               [org.slf4j/slf4j-log4j12]]]
                 [slingshot "0.12.2"]
                 [org.clojure/tools.logging "0.3.1"]]
  :repositories [["dice.repository"
                  {:url "https://raw.github.com/DICE-UNC/DICE-Maven/master/releases"}]])
