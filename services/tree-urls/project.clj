(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/tree-urls "4.0.3"
  :description "DE API for managing tree urls."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend"
  :license {:name "BSD"}
  :manifest {"Git-Ref" ~(git-ref)}
  :aot [tree-urls.core]
  :main tree-urls.core
  :uberjar-name "tree-urls-standalone.jar"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.iplantc/common-cli "4.0.3"]
                 [org.iplantc/common-cfg "4.0.3"]
                 [org.iplantc/kameleon "4.0.3"]
                 [me.raynes/fs "1.4.4"]
                 [cheshire "5.3.1"]
                 [compojure "1.1.6"]
                 [midje "1.6.3"]
                 [ring "1.2.1"]
                 [ring/ring-json "0.3.1"]]
  :iplant-rpm {:summary "DE API for managing tree urls."
               :provides "tree-urls"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]}
  :plugins [[lein-ring "0.8.10"]
            [lein-midje "3.1.1"]
            [org.iplantc/lein-iplant-rpm "4.0.3"]])
