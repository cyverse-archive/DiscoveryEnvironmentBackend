(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/data-info "4.0.4"
  :description "provides the data information HTTP API"
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "data-info-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.nrepl "0.2.6"]
                 [cheshire "5.3.1"]
                 [clj-time "0.8.0"]
                 [com.cemerick/url "0.1.1"]
                 [compojure "1.2.1"]
                 [dire "0.5.2"]
                 [liberator "0.12.2"]
                 [me.raynes/fs "1.4.6"]
                 [medley "0.5.3"]
                 [net.sf.json-lib/json-lib "2.4" :classifier "jdk15"]
                 [net.sf.opencsv/opencsv "2.3"]
                 [org.apache.tika/tika-core "1.6"]
                 [ring "1.3.1"]
                 [slingshot "0.12.1"]
                 [trptcolin/versioneer "0.1.1"]
                 [org.iplantc/clj-icat-direct "4.0.4"]
                 [org.iplantc/clj-jargon "4.0.4"
                  :exclusions [[xerces/xmlParserAPIs]
                               [org.irods.jargon.transfer/jargon-transfer-dao-spring]]]
                 [org.iplantc/clojure-commons "4.0.4"]
                 [org.iplantc/common-cli "4.0.4"]
                 [org.iplantc/heuristomancer "4.0.4"]]
  :plugins [[org.iplantc/lein-iplant-rpm "4.0.4"]
            [lein-ring "0.8.8"]
            [swank-clojure "1.4.2"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :aot [data-info.core]
  :main data-info.core
  :ring {:handler data-info.core/app
         :init data-info.core/lein-ring-init
         :port 31325
         :auto-reload? false}
  :iplant-rpm {:summary "iPlant Discovery Environment Data Information Services"
               :provides "data-info"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :exe-files ["resources/scripts/filetypes/guess-2.pl"]
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :uberjar-exclusions [#".*[.]SF" #"LICENSE" #"NOTICE"])
