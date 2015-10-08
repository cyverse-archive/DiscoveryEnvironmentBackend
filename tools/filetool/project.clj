(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/porklock "5.0.0"
  :description "A command-line tool for interacting with iRODS."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :main ^:skip-aot porklock.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "porklock-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [commons-io/commons-io "2.4"]
                 [slingshot "0.12.2"]
                 [org.iplantc/clj-jargon "5.0.0"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]])
