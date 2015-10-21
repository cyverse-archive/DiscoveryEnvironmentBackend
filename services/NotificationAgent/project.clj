(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/notificationagent "5.0.0"
  :description "A web service for storing and forwarding notifications."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "notificationagent-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.4.0"
                  :exclusions [[com.fasterxml.jackson.dataformat/jackson-dataformat-cbor]
                               [com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                               [com.fasterxml.jackson.core/jackson-annotations]
                               [com.fasterxml.jackson.core/jackson-databind]
                               [com.fasterxml.jackson.core/jackson-core]]]
                 [compojure "1.3.1"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/kameleon "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]
                 [me.raynes/fs "1.4.6"]
                 [clj-http "1.0.1"]
                 [clj-time "0.8.0"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [slingshot "0.12.1"]
                 [korma "0.3.2"]]
  :plugins [[lein-ring "0.8.13"]
            [lein-marginalia "0.7.0"]]
  :ring {:handler notification-agent.core/app
         :init notification-agent.core/load-config-from-file
         :port 31320}
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :extra-classpath-dirs ["conf/test"]
  :aot [notification-agent.core]
  :main notification-agent.core
  :uberjar-exclusions [#"(?i)[.]sf"]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/notificationagent-logging.xml"])
