(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/kifshare "4.0.5"
  :description "iPlant Quickshare for iRODS"
  :url "http://www.iplantcollaborative.org"

  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}

  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "kifshare-standalone.jar"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/core.memoize "0.5.3"]
                 [org.iplantc/clj-jargon "4.0.5"]
                 [org.iplantc/clojure-commons "4.0.5"]
                 [org.iplantc/common-cli "4.0.5"]
                 [me.raynes/fs "1.4.4"]
                 [cheshire "5.0.1"]
                 [slingshot "0.10.1"]
                 [compojure "1.1.3"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [ring/ring-devel "1.1.6"]
                 [de.ubercode.clostache/clostache "1.3.0"]
                 [com.cemerick/url "0.0.6"]
                 [lamina "0.5.0-beta9"]
                 [clj-http "0.6.5"]]

  :ring {:init kifshare.config/init
         :handler kifshare.core/app}

  :profiles {:dev {:resource-paths ["build"]
                   :dependencies [[midje "1.4.0"]]
                   :plugins [[lein-midje "2.0.1"]]}}

  :iplant-rpm {:summary "kifshare",
               :dependencies ["iplant-service-config >= 0.1.0-5"
                              "java-1.7.0-openjdk"],
               :config-files ["log4j.properties"],
               :config-path "conf"}

  :plugins [[lein-ring "0.7.5"]
            [org.iplantc/lein-iplant-rpm "4.0.5"]]

  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]

                 ["renci.repository"
                  {:url "http://ci-dev.renci.org/nexus/content/repositories/snapshots/"}]]

  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]]

  :aot [kifshare.core]
  :main kifshare.core)
