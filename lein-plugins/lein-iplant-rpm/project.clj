(defproject org.iplantc/lein-iplant-rpm "4.0.3"
  :eval-in-leiningen true
  :description "Leiningen Plugin for generating RPMs for Clojure projects."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [fleet "0.9.5"]]
  :repositories {"iplantCollaborative"
                 "http://katic.iplantcollaborative.org/archiva/repository/internal/"}
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
