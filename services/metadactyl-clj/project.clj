(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/metadactyl "3.2.10"
  :description "Framework for hosting DiscoveryEnvironment metadata services."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [cheshire "5.3.1"]
                 [clj-http "1.0.0"]
                 [clj-time "0.7.0"]
                 [com.cemerick/url "0.1.1"]
                 [com.google.guava/guava "18.0"]
                 [compojure "1.1.8"]
                 [medley "0.5.1"]
                 [metosin/compojure-api "0.15.0"]
                 [metosin/ring-swagger-ui "2.0.17"]
                 [org.iplantc/clojure-commons "3.2.10"]
                 [org.iplantc/kameleon "3.2.10"]
                 [org.iplantc/common-cli "3.2.10"]
                 [me.raynes/fs "1.4.6"]
                 [korma "0.3.2"]
                 [ring "1.2.2"]
                 [net.sf.json-lib/json-lib "2.4" :classifier "jdk15"]
                 [slingshot "0.10.3"]]
  :plugins [[org.iplantc/lein-iplant-rpm "3.2.10"]
            [lein-ring "0.8.10"]
            [lein-swank "1.4.4"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :aot [metadactyl.core]
  :main metadactyl.core
  :ring {:handler metadactyl.routes.api/app
         :init metadactyl.core/load-config-from-file
         :port 31323}
  :iplant-rpm {:summary "iPlant Discovery Environment Metadata Services"
               :provides "metadactyl"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :uberjar-exclusions [#"(?i)META-INF/[^/]*[.](SF|DSA|RSA)"]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
