(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/jex "4.2.0"
  :description "A backend job execution service that submits jobs to Condor."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "jex-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "3.4.0"]
                 [org.clojure/java.classpath "0.2.2"]
                 [cheshire "5.4.0"]
                 [com.cemerick/url "0.1.1"]
                 [compojure "1.3.2"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [org.iplantc/clojure-commons "4.2.0"]
                 [slingshot "0.12.2"]
                 [org.iplantc/common-cli "4.2.0"]
                 [org.iplantc/common-cfg "4.2.0"]
                 [me.raynes/fs "1.4.6"]]
  :plugins [[org.iplantc/lein-iplant-rpm "4.2.0"]
            [lein-midje "3.1.1"]]
  :iplant-rpm {:summary "jex",
               :runuser "condor"
               :dependencies ["iplant-service-config >= 0.1.0-5"
                              "java-1.7.0-openjdk"],
               :config-files ["log4j.properties"],
               :config-path "conf"
               :working-dir "pushd /var/lib/condor > /dev/null"}
  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}
  :aot [jex.core]
  :main jex.core
  :min-lein-version "2.0.0"
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]
                 ["renci.repository"
                  {:url "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
