(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/irods-avu-migrator "3.1.9"
  :description "DE tool for migrating AVU metadata from iRODS to PostgreSQL."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend"
  :license {:name "BSD"}
  :manifest {"Git-Ref" ~(git-ref)}
  :aot [irods-avu-migrator.core]
  :main irods-avu-migrator.core
  :uberjar-name "irods-avu-migrator.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.cli "0.3.1"]
                 [com.taoensso/timbre "3.1.6"]
                 [org.iplantc/common-cli "3.1.9"]
                 [org.iplantc/kameleon "3.1.9"]])
