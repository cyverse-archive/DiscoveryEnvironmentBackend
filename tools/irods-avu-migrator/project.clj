(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/irods-avu-migrator "3.2.2"
  :description "DE tool for migrating AVU metadata from iRODS to PostgreSQL."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend"
  :license {:name "BSD"}
  :manifest {"Git-Ref" ~(git-ref)}
  :aot [irods-avu-migrator.core]
  :main irods-avu-migrator.core
  :uberjar-name "irods-avu-migrator.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.iplantc/common-cli "3.2.2"]
                 [org.iplantc/kameleon "3.2.2"]])
