(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/kifshare "5.0.0"
  :description "iPlant Quickshare for iRODS"
  :url "http://www.iplantcollaborative.org"

  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}

  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "kifshare-standalone.jar"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [medley "0.5.5"]
                 [ring/ring-core "1.3.2"]
                 [org.iplantc/clj-jargon "5.0.0"
                   :exclusions [[org.slf4j/slf4j-log4j12]
                                [log4j]]]
                 [org.iplantc/service-logging "5.0.0"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.4.0"
                   :exclusions [[com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]
                                [com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                                [com.fasterxml.jackson.core/jackson-annotations]
                                [com.fasterxml.jackson.core/jackson-databind]
                                [com.fasterxml.jackson.core/jackson-core]]]
                 [slingshot "0.12.2"]
                 [compojure "1.3.4"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [ring/ring-devel "1.3.2"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [com.cemerick/url "0.1.1"]]

  :ring {:init kifshare.config/init
         :handler kifshare.core/app}

  :profiles {:dev     {:resource-paths ["build" "conf"]
                       :dependencies [[midje "1.6.3"]]
                       :plugins [[lein-midje "2.0.1"]]}
             :uberjar {:aot :all}}

  :plugins [[lein-ring "0.7.5"]]

  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]

                 ["renci.repository"
                  {:url "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"}]]

  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]]

  :main ^:skip-aot kifshare.core)
