(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/donkey "4.2.2-SNAPSHOT"
  :description "Framework for hosting DiscoveryEnvironment metadata services."
  :url "https://github.com/iPlantCollaborativeOpenSource/Donkey"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "donkey-standalone.jar"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/java.classpath "0.2.2"]
                 [org.apache.tika/tika-core "1.6"]
                 [org.iplantc/authy "4.2.2"]
                 [org.iplantc/clj-cas "4.2.2"]
                 [org.iplantc/clj-jargon "4.2.2" :exclusions [[xerces/xmlParserAPIs]]]
                 [org.iplantc/clojure-commons "4.2.2"]
                 [org.iplantc/mescal "4.2.2"]
                 [org.iplantc/kameleon "4.2.2"]
                 [org.iplantc/heuristomancer "4.2.2"]
                 [org.iplantc/clj-icat-direct "4.2.2"]
                 [org.iplantc/common-cli "4.2.2"]
                 [org/forester "1.005" ]
                 [org.nexml.model/nexml "1.5-SNAPSHOT"]
                 [net.sf.json-lib/json-lib "2.4" :classifier "jdk15"]
                 [cheshire "5.4.0"]
                 [clj-http "1.0.1"]
                 [clj-time "0.8.0"]
                 [com.cemerick/url "0.1.1"]
                 [ring "1.3.2"]
                 [compojure "1.3.1"]
                 [clojurewerkz/elastisch "2.1.0"]
                 [com.novemberain/validateur "2.4.2"]
                 [com.novemberain/welle "3.0.0"]
                 [byte-streams "0.1.13"]
                 [xerces/xercesImpl "2.11.0"]
                 [commons-net "3.3"]
                 [org.clojure/tools.nrepl "0.2.6"]
                 [net.sf.opencsv/opencsv "2.3"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [me.raynes/fs "1.4.6"]
                 [medley "0.5.3"]
                 [dire "0.5.3"]
                 [mvxcvi/clj-pgp "0.8.0"]
                 [prismatic/schema "0.3.3"]
                 [slingshot "0.12.1"]]
  :plugins [[org.iplantc/lein-iplant-rpm "4.2.2"]
            [lein-ring "0.8.8"]
            [swank-clojure "1.4.2"]]
  :profiles {:dev     {:resource-paths ["conf/test"]}
             :uberjar {:aot :all}}
  :main ^:skip-aot donkey.core
  :ring {:handler donkey.core/app
         :init donkey.core/lein-ring-init
         :port 31325
         :auto-reload? false}
  :iplant-rpm {:summary "iPlant Discovery Environment Business Layer Services"
               :provides "donkey"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :exe-files ["resources/scripts/filetypes/guess-2.pl"]
               :config-files ["log4j.properties"]
               :config-path "conf/main"}
  :uberjar-exclusions [#".*[.]SF" #"LICENSE" #"NOTICE"]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]
                 ["biojava"
                  {:url "http://www.biojava.org/download/maven"}]
                 ["nexml"
                  {:url "http://nexml-dev.nescent.org/.m2/repository"
                   :checksum :ignore
                   :update :never}]])
