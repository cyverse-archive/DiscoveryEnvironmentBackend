(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/panopticon "4.0.0"
  :description "A background service for monitoring the statuses of Condor jobs."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.iplantc/clojure-commons "4.0.0"]
                 [org.iplantc/common-cli "4.0.0"]
                 [me.raynes/fs "1.4.4"]
                 [cheshire "5.0.1"]
                 [clj-time "0.4.4"]
                 [slingshot "0.10.3"]]
  :plugins [[org.iplantc/lein-iplant-rpm "4.0.0"]
            [lein-midje "3.1.1"]]
  :iplant-rpm {:summary "panopticon"
               :runuser "condor"
               :dependencies ["iplant-service-config >= 0.1.0-5"
                              "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "conf"}
  :profiles {:dev {:dependencies [[midje "1.6.0"]]}}
  :aot [panopticon.core]
  :main panopticon.core
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
