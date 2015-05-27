(defproject org.iplantc/lein-iplant-rpm "5.0.0"
  :eval-in-leiningen true
  :description "Leiningen Plugin for generating RPMs for Clojure projects."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [fleet "0.10.2"]]
  :repositories {"iplantCollaborative"
                 "https://everdene.iplantcollaborative.org/archiva/repository/internal/"}
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
