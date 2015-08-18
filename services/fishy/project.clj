(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject fishy "5.0.0"
  :description "A REST front-end for Grouper."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "fishy-standalone.jar"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [clj-time "0.10.0"]
                 [com.cemerick/url "0.1.1"]
                 [medley "0.7.0"]
                 [metosin/compojure-api "0.22.1"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]]
  :plugins [[lein-ring "0.9.6"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :aot [fishy.core]
  :main fishy.core
  :ring {:handler fishy.routes/app
         :init    fishy.core/init-service
         :port    31310}
  :uberjar-exclusions [#"(?i)META-INF/[^/]*[.](SF|DSA|RSA)"])
