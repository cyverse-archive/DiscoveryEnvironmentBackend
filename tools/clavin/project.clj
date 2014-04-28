(defproject org.iplantc/clavin "3.1.1"
  :description "A command-line tool for loading service configurations into Zookeeper."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/Clavin.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/Clavin.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/Clavin.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.antlr/stringtemplate "4.0.2"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.1"]
                 [medley "0.1.5"]
                 [me.raynes/fs "1.4.4"]
                 [org.iplantc/clojure-commons "3.1.1"]
                 [zookeeper-clj "0.9.1"]]
  :plugins [[org.iplantc/lein-iplant-cmdtar "3.1.1"]
            [org.iplantc/lein-iplant-rpm "3.1.1"]]
  :iplant-rpm {:summary "Clavin"
               :type :command
               :provides "iplant-clavin"}
  :aot [clavin.core]
  :main clavin.core
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
