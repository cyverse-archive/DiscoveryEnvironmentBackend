(defproject org.iplantc/osm "1.1.2"
  :description "An object state management system."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/OSM.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/OSM.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/OSM.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [cheshire "5.0.2"]
                 [congomongo "0.4.1"]
                 [compojure "1.1.5"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [swank-clojure "1.4.3"]
                 [org.iplantc/clojure-commons "1.4.8"]
                 [org.apache.httpcomponents/httpcore "4.2.3"]
                 [org.apache.httpcomponents/httpclient "4.2.3"]
                 [log4j/log4j "1.2.17"]
                 [slingshot "0.10.3"]]
  :plugins [[org.iplantc/lein-iplant-rpm "1.4.3"]]
  :iplant-rpm {:summary "osm"
               :dependencies ["iplant-service-config >= 0.1.0-5"
                              "iplant-clavin"
                              "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "conf"}
  :aot [osm.core]
  :main osm.core
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
