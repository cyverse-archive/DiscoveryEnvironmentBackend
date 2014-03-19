(defproject org.iplantc/conrad "1.3.3"
  :description "Back-End Services for the iPlant Administrative Console"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/Conrad.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/Conrad.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/Conrad.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.iplantc/clj-cas "1.8.4"]
                 [org.iplantc/kameleon "1.8.6"]
                 [org.iplantc/clojure-commons "1.4.9"]
                 [cheshire "5.0.2"]
                 [compojure "1.1.5"]
                 [swank-clojure "1.4.3"]
                 [log4j/log4j "1.2.17"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [korma/korma "0.3.0-RC4"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [postgresql/postgresql "9.0-801.jdbc4"]]
  :plugins [[org.iplantc/lein-iplant-rpm "1.4.3"]
            [lein-ring "0.8.3"]
            [lein-swank "1.4.5"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :aot [conrad.core]
  :main conrad.core
  :ring {:handler conrad.core/app
         :init conrad.core/load-configuration-from-file
         :port 31334}
  :iplant-rpm {:summary "iPlant Conrad"
               :provides "conrad"
               :dependencies ["iplant-service-config >= 0.1.0-5" "iplant-clavin" "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
