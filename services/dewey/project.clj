(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/dewey "5.0.0"
  :description "This is a RabbitMQ client responsible for keeping an elasticsearch index
                synchronized with an iRODS repository using messages produced by iRODS."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "dewey-standalone.jar"
  :main ^:skip-aot dewey.core
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [cheshire "5.3.1"
                   :exclusions [[com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]
                                [com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                                [com.fasterxml.jackson.core/jackson-annotations]
                                [com.fasterxml.jackson.core/jackson-databind]
                                [com.fasterxml.jackson.core/jackson-core]]]
                 [clojurewerkz/elastisch "2.0.0"]
                 [com.novemberain/langohr "2.11.0"]
                 [liberator "0.11.1"]
                 [compojure "1.1.8"]
                 [ring/ring-core "1.3.0"]
                 [ring/ring-jetty-adapter "1.3.0"]
                 [slingshot "0.10.3"]
                 [org.iplantc/clj-jargon "5.0.0"
                   :exclusions [[org.slf4j/slf4j-log4j12]
                                [log4j]]]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]
                 [me.raynes/fs "1.4.6"]]
  :resource-paths []
  :profiles {:dev     {:dependencies   [[midje "1.6.3"]]
                       :resource-paths ["dev-resource"]}
             :uberjar {:aot :all}}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
