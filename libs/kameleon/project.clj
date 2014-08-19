(defproject org.iplantc/kameleon "3.2.3"
  :description "Library for interacting with backend relational databases."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [korma "0.3.0-RC5"]
                 [postgresql "9.0-801.jdbc4"]
                 [slingshot "0.10.3"]]
  :plugins [[lein-marginalia "0.7.1"]]
  :manifest {"db-version" "2.0.0:20140902.01"}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
