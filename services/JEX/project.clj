(defproject org.iplantc/jex "0.3.1"
  :description "A backend job execution service that submits jobs to Condor."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/JEX.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/JEX.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/JEX.git"}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/java.classpath "0.1.0"]
                 [cheshire "5.0.1"]
                 [compojure "1.0.1"]
                 [ring/ring-jetty-adapter "1.0.1"]
                 [org.iplantc/clojure-commons "1.4.8"]
                 [slingshot "0.10.3"]]
  :plugins [[org.iplantc/lein-iplant-rpm "1.4.3"]]
  :iplant-rpm {:summary "jex",
               :runuser "condor"
               :dependencies ["iplant-service-config >= 0.1.0-5"
                              "iplant-clavin"
                              "java-1.7.0-openjdk"],
               :config-files ["log4j.properties"],
               :config-path "conf"}
  :profiles {:dev {:dependencies [[midje "1.6.0"]
                                  [lein-midje "3.1.1"]]}}
  :aot [jex.core]
  :main jex.core
  :min-lein-version "2.0.0"
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]
                 ["renci.repository"
                  {:url "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
