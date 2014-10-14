(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/dewey "4.0.1"
  :description "This is a RabbitMQ client responsible for keeping an elasticsearch index
                synchronized with an iRODS repository using messages produced by iRODS."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :aot [dewey.core]
  :main dewey.core
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.0"]
                 [cheshire "5.3.1"]
                 [clojurewerkz/elastisch "2.0.0"]
                 [com.novemberain/langohr "2.11.0"]
                 [liberator "0.11.1"]
                 [compojure "1.1.8"]
                 [ring/ring-core "1.3.0"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [slingshot "0.10.3"]
                 [org.iplantc/clj-jargon "4.0.1"]
                 [org.iplantc/clojure-commons "4.0.1"]
                 [org.iplantc/common-cli "4.0.1"]
                 [me.raynes/fs "1.4.6"]]
  :resource-paths []
  :profiles {:dev {:dependencies   [[midje "1.6.3"]]
                   :resource-paths ["dev-resource"]}}
  :plugins [[org.iplantc/lein-iplant-rpm "4.0.1"]]
  :iplant-rpm {:summary      "dewey"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path  "resources"}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
