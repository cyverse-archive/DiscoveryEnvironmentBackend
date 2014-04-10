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
                 [org.iplantc/common-cli "3.0.2"]
                 [com.taoensso/timbre "3.1.6"]
                 [bouncer "0.3.1-beta1"]
                 [medley "0.1.5"]
                 [me.raynes/fs "1.4.4"]
                 [compojure "1.1.6"]
                 [ring "1.2.1"]]
  :plugins [[lein-ring "0.8.10"]])
