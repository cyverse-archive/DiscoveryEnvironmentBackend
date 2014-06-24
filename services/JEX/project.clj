(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/jex "3.1.4"
  :description "A backend job execution service that submits jobs to Condor."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/JEX.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/JEX.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/JEX.git"}
  :manifest {"Git-Ref" ~(git-ref)}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/java.classpath "0.1.0"]
                 [cheshire "5.0.1"]
                 [compojure "1.0.1"]
                 [ring/ring-jetty-adapter "1.0.1"]
                 [org.iplantc/clojure-commons "3.1.4"]
                 [slingshot "0.10.3"]
                 [me.raynes/fs "1.4.4"]
                 [org.iplantc/common-cli "3.1.4"]]
  :plugins [[org.iplantc/lein-iplant-rpm "3.1.4"]
            [lein-midje "3.1.1"]]
  :iplant-rpm {:summary "jex",
               :runuser "condor"
               :dependencies ["iplant-service-config >= 0.1.0-5"
                              "java-1.7.0-openjdk"],
               :config-files ["log4j.properties"],
               :config-path "conf"}
  :profiles {:dev {:dependencies [[midje "1.6.0"]]}}
  :aot [jex.core]
  :main jex.core
  :min-lein-version "2.0.0"
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]
                 ["renci.repository"
                  {:url "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
