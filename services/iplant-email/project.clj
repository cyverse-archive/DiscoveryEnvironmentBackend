(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/iplant-email "4.0.0"
  :description "iPlant Email Service"
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :scm {:connection "scm:git:git@github.com:iPlantCollaborativeOpenSource/iplant-email.git"
        :developerConnection "scm:git:git@github.com:iPlantCollaborativeOpenSource/iplant-email.git"
        :url "git@github.com:iPlantCollaborativeOpenSource/iplant-email.git"}
  :manifest {"Git-Ref" ~(git-ref)}
  :pom-addition [:developers
                 [:developer
                  [:url "https://github.com/orgs/iPlantCollaborativeOpenSource/teams/iplant-devs"]]]
  :classifiers [["javadoc" :javadoc]
                ["sources" :sources]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.iplantc/clojure-commons "4.0.0"]
                 [org.iplantc/common-cli "4.0.0"]
                 [org.clojure/tools.logging "0.2.3"]
                 [cheshire "5.0.1"]
                 [javax.mail/mail "1.4"]
                 [org.bituf/clj-stringtemplate "0.2"]
                 [compojure "1.0.1"]
                 [ring/ring-jetty-adapter "1.0.1"]
                 [log4j/log4j "1.2.16"]
                 [org.clojure/tools.cli "0.3.1"]]
  :plugins [[org.iplantc/lein-iplant-rpm "4.0.0"]]
  :iplant-rpm {:summary "iplant-email"
               :dependencies ["iplant-service-config >= 0.1.0-5"
                              "iplant-clavin"
                              "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "conf"
               :resources ["*.st"]}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]]
  :aot [iplant-email.core]
  :main iplant-email.core)
