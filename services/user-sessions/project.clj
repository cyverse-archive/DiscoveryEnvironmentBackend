(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/user-sessions "4.2.1"
  :description "DE API for managing user sessions."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend"
  :license {:name "BSD"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "user-sessions-standalone.jar"
  :aot [user-sessions.core]
  :main user-sessions.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.iplantc/common-cli "4.2.1"]
                 [org.iplantc/common-cfg "4.2.1"]
                 [org.iplantc/kameleon "4.2.1"]
                 [me.raynes/fs "1.4.4"]
                 [cheshire "5.3.1"]
                 [compojure "1.1.6"]
                 [midje "1.6.3"]
                 [ring "1.2.1"]
                 [ring/ring-json "0.3.1"]]
  :iplant-rpm {:summary "DE API for managing user sessions."
               :provides "user-sessions"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]}
  :plugins [[lein-ring "0.8.10"]
            [lein-midje "3.1.1"]
            [org.iplantc/lein-iplant-rpm "4.2.1"]])
