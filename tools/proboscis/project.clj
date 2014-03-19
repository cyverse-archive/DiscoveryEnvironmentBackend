(defproject org.iplantc/proboscis "0.2.1"
  :description "A utility for creating an ElasticSearch index and its mappings for Infosquito."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD License"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/proboscis.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/proboscis.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/proboscis.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [cheshire "5.2.0"]
                 [clojurewerkz/elastisch "1.2.0"]
                 [slingshot "0.10.3"]]
  :resource-paths ["config"]
  :plugins [[org.iplantc/lein-iplant-cmdtar "0.1.2"]]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]]
  :aot [proboscis.core]
  :main proboscis.core)
