(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject template-mover "5.0.0"
  :description "Utility to copy metadata templates to their new database."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "template-mover-standalone.jar"
  :dependencies [[honeysql "0.6.0"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.iplantc/kameleon "5.0.0"]
                 [postgresql "9.1-901-1.jdbc4"]]
  :aot :all
  :main template-mover.core)
