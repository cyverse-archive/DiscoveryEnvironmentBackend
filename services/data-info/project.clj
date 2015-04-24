(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/data-info "4.2.5"
  :description "provides the data information HTTP API"
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "data-info-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.memoize "0.5.7"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.nrepl "0.2.9"]
                 [cheshire "5.4.0"]
                 [clj-time "0.9.0"]
                 [com.cemerick/url "0.1.1"]
                 [compojure "1.3.2"]
                 [dire "0.5.3"]
                 [liberator "0.12.2"]
                 [me.raynes/fs "1.4.6"]
                 [medley "0.5.5"]
                 [net.sf.json-lib/json-lib "2.4" :classifier "jdk15"]
                 [net.sf.opencsv/opencsv "2.3"]
                 [org.apache.tika/tika-core "1.7"]
                 [ring "1.3.2"]
                 [slingshot "0.12.2"]
                 [trptcolin/versioneer "0.1.1"]
                 [org.iplantc/clj-icat-direct "4.2.5"]
                 [org.iplantc/clj-jargon "4.2.5"]
                 [org.iplantc/clojure-commons "4.2.5"]
                 [org.iplantc/common-cli "4.2.5"]
                 [org.iplantc/common-cfg "4.2.5"]
                 [org.iplantc/heuristomancer "4.2.5"]
                 [org.iplantc/kameleon "4.2.5"]]
  :plugins [[org.iplantc/lein-iplant-rpm "4.2.5"]
            [lein-ring "0.8.8"]
            [swank-clojure "1.4.2"]]
  :profiles {:dev     {:resource-paths ["conf/test"]}
             :uberjar {:aot :all}}
  :main ^:skip-aot data-info.core
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
