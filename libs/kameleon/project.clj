(defproject org.iplantc/kameleon "4.1.5"
  :description "Library for interacting with backend relational databases."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [korma "0.3.3"]
                 [me.raynes/fs "1.4.6"]
                 [postgresql "9.1-901-1.jdbc4"]
                 [slingshot "0.10.3"]]
  :plugins [[lein-marginalia "0.7.1"]]
  :manifest {"db-version" "1.9.5:20150202.01"}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
