(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/anon-files "5.0.0"
  :description "Serves up files and directories that are shared with the anonymous user in iRODS."
  :url "http://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend/"
  :license {:name "BSD"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "anon-files-standalone.jar"
  :main ^:skip-aot anon-files.core
  :profiles {:uberjar {:aot :all}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.iplantc/clj-jargon "5.0.0"
                  :exclusions [[org.slf4j/slf4j-log4j12]
                               [log4j]]]
                 [org.iplantc/service-logging "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/common-cfg "5.0.0"]
                 [medley "0.6.0"]
                 [compojure "1.3.4"]
                 [ring "1.3.2"]]
  :plugins [[lein-ring "0.9.3"]])
