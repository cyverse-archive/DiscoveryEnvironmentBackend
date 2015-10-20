(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/metadata "5.0.0"
  :description "The REST API for the Discovery Environment Metadata services."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentBackend"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [metosin/compojure-api "0.23.1"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/common-cfg "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/common-swagger-api "5.0.0"]
                 [org.iplantc/kameleon "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]
                 [slingshot "0.12.2"]]
  :main metadata.core
  :ring {:handler metadata.routes/app
         :init    metadata.core/init-service
         :port    60000}
  :uberjar-name "metadata-standalone.jar"
  :profiles {:dev     {:plugins        [[lein-ring "0.9.6"]]
                       :resource-paths ["conf/test"]}
             ;; compojure-api route macros should not be AOT compiled:
             ;; https://github.com/metosin/compojure-api/issues/135#issuecomment-121388539
             ;; https://github.com/metosin/compojure-api/issues/102
             :uberjar {:aot [#"metadata.(?!routes).*"]}}
  :uberjar-exclusions [#".*[.]SF" #"LICENSE" #"NOTICE"]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/metadata-logging.xml"])
