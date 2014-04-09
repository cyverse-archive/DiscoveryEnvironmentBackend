(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/anon-files "3.0.2"
  :description "Serves up files and directories that are shared with the anonymous user in iRODS."
  :url "http://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend/"
  :license {:name "BSD"}
  :manifest {"Git-Ref" ~(git-ref)}
  :aot [anon-files.core]
  :main anon-files.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.iplantc/clj-jargon "3.0.2"]
                 [org.iplantc/clojure-commons "3.0.2"]
                 [org.iplantc/common-cli "3.0.2"]
                 [compojure "1.1.6"]
                 [ring "1.2.1"]])
