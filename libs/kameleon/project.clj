(defproject org.iplantc/kameleon "5.0.0"
  :description "Library for interacting with backend relational databases."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-time "0.11.0"]
                 [korma "0.4.2"]
                 [me.raynes/fs "1.4.6"]
                 [postgresql "9.3-1102.jdbc41"]
                 [slingshot "0.12.2"]]
  :plugins [[lein-marginalia "0.7.1"]]
  :manifest {"db-version" "2.1.0:20150903.01"}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
