(defproject org.iplantc/metadactyl "3.1.7"
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
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [cheshire "5.3.1"]
                 [clj-time "0.7.0"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [com.cemerick/url "0.1.1"]
                 [compojure "1.1.7"]
                 [medley "0.1.5"]
                 [org.iplantc/clojure-commons "3.1.7"]
                 [org.iplantc/kameleon "3.1.7"]
                 [org.iplantc.core/metadactyl "3.1.7"]
                 [org.iplantc/common-cli "3.1.7"]
                 [me.raynes/fs "1.4.5"]
                 [org.springframework/spring-orm "3.1.0.RELEASE"]
                 [korma "0.3.0-RC5"]
                 [ring "1.2.2"]
                 [net.sf.json-lib/json-lib "2.4" :classifier "jdk15"]
                 [slingshot "0.10.3"]]
  :plugins [[org.iplantc/lein-iplant-rpm "3.1.7"]
            [lein-ring "0.8.10"]
            [lein-swank "1.4.4"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :aot [metadactyl.core]
  :main metadactyl.core
  :ring {:handler metadactyl.core/app
         :init metadactyl.core/load-config-from-file
         :port 31323}
  :iplant-rpm {:summary "iPlant Discovery Environment Metadata Services"
               :provides "metadactyl"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :uberjar-exclusions [#"(?i)META-INF/[^/]*[.](SF|DSA|RSA)"]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
