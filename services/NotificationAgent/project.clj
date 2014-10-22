(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/notificationagent "3.2.9"
  :description "A web service for storing and forwarding notifications."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [cheshire "5.0.1"]
                 [compojure "1.0.2"]
                 [org.iplantc/clojure-commons "3.2.9"]
                 [org.iplantc/kameleon "3.2.9"]
                 [org.iplantc/common-cli "3.2.9"]
                 [me.raynes/fs "1.4.4"]
                 [clj-http "0.5.5"]
                 [clj-time "0.5.0"]
                 [ring/ring-jetty-adapter "1.1.0"]
                 [org.codehaus.jackson/jackson-core-asl "1.9.5"]
                 [slingshot "0.10.3"]
                 [korma "0.3.0-RC5"]]
  :plugins [[lein-ring "0.6.4"]
            [swank-clojure "1.4.2"]
            [lein-marginalia "0.7.0"]
            [org.iplantc/lein-iplant-rpm "3.2.9"]]
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
