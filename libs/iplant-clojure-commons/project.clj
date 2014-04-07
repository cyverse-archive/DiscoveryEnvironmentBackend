(defproject org.iplantc/clojure-commons "4.0.0"
  :description "Common Utilities for Clojure Projects"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/iplant-clojure-commons.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/iplant-clojure-commons.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/iplant-clojure-commons.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [zookeeper-clj "0.9.1"]
                 [clj-http "0.6.5"]
                 [clj-http-fake "0.4.1"]
                 [com.cemerick/url "0.0.7"]
                 [log4j/log4j "1.2.17"]
                 [slingshot "0.10.3"]
                 [org.mongodb/mongo-java-driver "2.10.1"]
                 [org.apache.httpcomponents/httpclient "4.2.3"]
                 [commons-configuration/commons-configuration "1.8"]
                 [cheshire "5.0.2"]
                 [trptcolin/versioneer "0.1.0"]]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]]
  :profiles {:test {:resource-paths ["resources" "test-resources"]}})
