(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/porklock "4.1.2"
  :description "A command-line tool for interacting with iRODS."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :main porklock.core
  :aot [porklock.core]
  :uberjar-name "porklock-standalone.jar"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.iplantc/clojure-commons "4.1.2"]
                 [org.clojure/tools.cli "0.2.1"]
                 [commons-io/commons-io "2.2"]
                 [slingshot "0.10.3"]
                 [com.cemerick/url "0.0.7"]
                 [com.novemberain/welle "1.4.0"]
                 [cheshire "5.1.1"]
                 [org.iplantc/clj-jargon "4.1.2"]]
  :plugins [[org.iplantc/lein-iplant-rpm "4.1.2"]]
  :iplant-rpm {:summary "Porklock"
               :type :command
               :exe-files ["curl_wrapper.pl"]}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
