(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/notificationagent "4.1.6"
  :description "A web service for storing and forwarding notifications."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "notificationagent-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [cheshire "5.4.0"]
                 [compojure "1.3.1"]
                 [org.iplantc/clojure-commons "4.1.6"]
                 [org.iplantc/kameleon "4.1.6"]
                 [org.iplantc/common-cli "4.1.6"]
                 [me.raynes/fs "1.4.6"]
                 [clj-http "1.0.1"]
                 [clj-time "0.8.0"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [slingshot "0.12.1"]
                 [korma "0.3.2"]]
  :plugins [[lein-ring "0.8.13"]
            [lein-marginalia "0.7.0"]
            [org.iplantc/lein-iplant-rpm "4.1.6"]]
  :ring {:handler notification-agent.core/app
         :init notification-agent.core/load-config-from-file
         :port 31320}
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :extra-classpath-dirs ["conf/test"]
  :iplant-rpm {:summary "iPlant Notification Agent"
               :provides "notificationagent"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :aot [notification-agent.core]
  :main notification-agent.core
  :uberjar-exclusions [#"(?i)[.]sf"]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]])
