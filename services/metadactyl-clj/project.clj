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
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.cemerick/url "0.1.1"]
                 [com.google.guava/guava "18.0"]
                 [medley "0.7.0"]
                 [metosin/compojure-api "0.23.1"]
                 [org.iplantc/authy "5.0.0"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/kameleon "5.0.0"]
                 [org.iplantc/mescal "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/common-cfg "5.0.0"]
                 [org.iplantc/common-swagger-api "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]
                 [me.raynes/fs "1.4.6"]
                 [mvxcvi/clj-pgp "0.8.0"]]
  :plugins [[lein-ring "0.9.6"]
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
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/metadactyl-logging.xml"])
