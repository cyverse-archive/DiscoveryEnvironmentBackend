(defproject org.iplantc/panopticon "1.0.2"
  :description "A background service for monitoring the statuses of Condor jobs."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/Panopticon.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/Panopticon.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/Panopticon.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.iplantc/clojure-commons "1.4.8"]
                 [cheshire "5.0.1"]
                 [clj-time "0.4.4"]
                 [slingshot "0.10.3"]]
  :plugins [[org.iplantc/lein-iplant-rpm "1.4.3"]]
  :iplant-rpm {:summary "panopticon"
               :runuser "condor"
               :dependencies ["iplant-service-config >= 0.1.0-5"
                              "iplant-clavin"
                              "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "conf"}
  :aot [panopticon.core]
  :main panopticon.core
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
