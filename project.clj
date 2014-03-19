(defproject org.iplantc/metadactyl "1.8.7"
  :description "Framework for hosting DiscoveryEnvironment metadata services."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/metadactyl-clj.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/metadactyl-clj.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/metadactyl-clj.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [cheshire "5.0.1"]
                 [clj-time "0.4.4"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [com.cemerick/url "0.1.1"]
                 [compojure "1.1.5"]
                 [medley "0.1.5"]
                 [org.iplantc/clojure-commons "1.4.9"]
                 [org.iplantc/kameleon "1.8.6"]
                 [org.iplantc.core/metadactyl "1.8.6"]
                 [org.springframework/spring-orm "3.1.0.RELEASE"]
                 [korma "0.3.0-RC5"]
                 [ring "1.1.8"]
                 [org.slf4j/slf4j-api "1.5.8"]
                 [org.slf4j/slf4j-log4j12 "1.5.8"]
                 [net.sf.json-lib/json-lib "2.4" :classifier "jdk15"]
                 [slingshot "0.10.3"]]
  :plugins [[org.iplantc/lein-iplant-rpm "1.4.3"]
            [lein-ring "0.8.5"]
            [lein-swank "1.4.4"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :aot [metadactyl.core]
  :main metadactyl.core
  :ring {:handler metadactyl.core/app
         :init metadactyl.core/load-config-from-file}
  :iplant-rpm {:summary "iPlant Discovery Environment Metadata Services"
               :provides "metadactyl"
               :dependencies ["iplant-service-config >= 0.1.0-5" "iplant-clavin" "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :uberjar-exclusions [#"(?i)META-INF/[^/]*[.](SF|DSA|RSA)"]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
