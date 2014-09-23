(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/anon-files "3.2.7"
  :description "Serves up files and directories that are shared with the anonymous user in iRODS."
  :url "http://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend/"
  :license {:name "BSD"}
  :manifest {"Git-Ref" ~(git-ref)}
  :aot [anon-files.core]
  :main anon-files.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.iplantc/clj-jargon "3.2.7"]
                 [org.iplantc/common-cli "3.2.7"]
                 [org.iplantc/common-cfg "3.2.7"]
                 [medley "0.1.5"]
                 [me.raynes/fs "1.4.4"]
                 [compojure "1.1.6"]
                 [ring "1.2.1"]]
  :iplant-rpm {:summary "Serves up files and directories that are shared with the anonymous user in iRODS."
               :provides "anon-files"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "resources/main"}
  :plugins [[lein-ring "0.8.10"]
            [org.iplantc/lein-iplant-rpm "3.2.7"]])
