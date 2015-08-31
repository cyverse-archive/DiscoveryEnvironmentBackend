(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/iplant-groups "5.0.0"
  :description "A REST front-end for Grouper."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "iplant-groups-standalone.jar"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [clj-time "0.10.0"]
                 [com.cemerick/url "0.1.1"]
                 [medley "0.7.0"]
                 [metosin/compojure-api "0.22.1"]
                 [me.raynes/fs "1.4.6"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/common-cfg "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]]
  :plugins [[lein-ring "0.9.6"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  ;; compojure-api route macros should not be AOT compiled:
  ;; https://github.com/metosin/compojure-api/issues/135#issuecomment-121388539
  ;; https://github.com/metosin/compojure-api/issues/102
  :aot [#"iplant_groups.(?!routes).*"]
  :main iplant_groups.core
  :ring {:handler iplant_groups.routes/app
         :init    iplant_groups.core/init-service
         :port    31310}
  :uberjar-exclusions [#"(?i)META-INF/[^/]*[.](SF|DSA|RSA)"])
