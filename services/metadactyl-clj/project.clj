(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/metadactyl "5.0.0"
  :description "Framework for hosting DiscoveryEnvironment metadata services."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "metadactyl-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.4.0"
                   :exclusions [[com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]
                                [com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                                [com.fasterxml.jackson.core/jackson-annotations]
                                [com.fasterxml.jackson.core/jackson-databind]
                                [com.fasterxml.jackson.core/jackson-core]]]
                 [clj-http "1.0.0"]
                 [clj-time "0.7.0"]
                 [com.cemerick/url "0.1.1"]
                 [com.google.guava/guava "18.0"]
                 [compojure "1.3.4"]
                 [medley "0.5.1"]
                 [metosin/compojure-api "0.21.0"]
                 [org.iplantc/authy "5.0.0"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/kameleon "5.0.0"]
                 [org.iplantc/mescal "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/common-cfg "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]
                 [me.raynes/fs "1.4.6"]
                 [mvxcvi/clj-pgp "0.8.0"]
                 [korma "0.3.2"]
                 [ring "1.2.2"]
                 [net.sf.json-lib/json-lib "2.4" :classifier "jdk15"]
                 [slingshot "0.10.3"]]
  :plugins [[lein-ring "0.9.4"]
            [lein-swank "1.4.4"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  ;; compojure-api route macros should not be AOT compiled:
  ;; https://github.com/metosin/compojure-api/issues/135#issuecomment-121388539
  ;; https://github.com/metosin/compojure-api/issues/102
  :aot [#"metadactyl.(?!routes).*"]
  :main metadactyl.core
  :ring {:handler metadactyl.routes.api/app
         :init metadactyl.core/load-config-from-file
         :port 31323}
  :uberjar-exclusions [#"(?i)META-INF/[^/]*[.](SF|DSA|RSA)"]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
