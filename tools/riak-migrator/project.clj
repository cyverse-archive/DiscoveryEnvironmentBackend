(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/riak-migrator "3.1.4"
  :description "DE tool for migrating data from Riak to PostgreSQL."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend"
  :license {:name "BSD"}
  :manifest {"Git-Ref" ~(git-ref)}
  :aot [riak-migrator.core]
  :main riak-migrator.core
  :uberjar-name "riak-migrator.jar"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.9.1"]
                 [cheshire "5.3.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.iplantc/common-cli "3.1.4"]
                 [org.iplantc/kameleon "3.1.4"]
                 [com.cemerick/url "0.1.1"]
                 [me.raynes/fs "1.4.4"]])
