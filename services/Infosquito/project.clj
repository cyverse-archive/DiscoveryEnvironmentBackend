(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/infosquito "5.0.0"
  :description "An ICAT database crawler used to index the contents of iRODS."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "infosquito-standalone.jar"
  :aot [infosquito.core]
  :main infosquito.core
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [postgresql "9.1-901-1.jdbc4"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [cheshire "5.3.1"
                  :exclusions [[com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]
                               [com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                               [com.fasterxml.jackson.core/jackson-annotations]
                               [com.fasterxml.jackson.core/jackson-databind]
                               [com.fasterxml.jackson.core/jackson-core]]]
                 [clojurewerkz/elastisch "2.0.0"]
                 [com.novemberain/langohr "2.11.0"]
                 [slingshot "0.10.3"]
                 [me.raynes/fs "1.4.6"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]]
  :profiles {:dev {:resource-paths ["dev-resources"]}}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]
                        ["renci.repository"
                         {:url "http://ci-dev.renci.org/nexus/content/repositories/releases/"}]])
