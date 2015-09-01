(use '[clojure.java.shell :only (sh)])
(require '[clojure.string :as string])

(defn git-ref
  []
  (or (System/getenv "GIT_COMMIT")
      (string/trim (:out (sh "git" "rev-parse" "HEAD")))
      ""))

(defproject org.iplantc/donkey "5.0.0-SNAPSHOT"
  :description "Framework for hosting DiscoveryEnvironment metadata services."
  :url "https://github.com/iPlantCollaborativeOpenSource/Donkey"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :manifest {"Git-Ref" ~(git-ref)}
  :uberjar-name "donkey-standalone.jar"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.memoize "0.5.7"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [byte-streams "0.2.0"
                  :exclusions [[clj-tuple]
                               [riddley]]]
                 [cheshire "5.5.0"]
                 [clj-http "2.0.0"]
                 [clj-time "0.11.0"]
                 [clojurewerkz/elastisch "2.1.0"]
                 [com.cemerick/url "0.1.1" :exclusions [com.cemerick/clojurescript.test]]
                 [commons-net "3.3"]                               ; provides org.apache.commons.net
                 [compojure "1.4.0"]
                 [de.ubercode.clostache/clostache "1.4.0" :exclusions [org.clojure/core.incubator]]
                 [dire "0.5.3"]
                 [me.raynes/fs "1.4.6"]
                 [medley "0.7.0"]
                 [net.sf.opencsv/opencsv "2.3"]
                 [org.apache.tika/tika-core "1.10"]      ; provides org.apache.tika
                 [org.nexml.model/nexml "1.5-SNAPSHOT"]  ; provides org.nexml.model
                 [org/forester "1.005" ]
                 [ring "1.4.0"]
                 [slingshot "0.12.2"]
                 [org.iplantc/clj-cas "5.0.0"]
                 [org.iplantc/clj-icat-direct "5.0.0"]
                 [org.iplantc/clj-jargon "5.0.0"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/common-cfg "5.0.0"]
                 [org.iplantc/common-cli "5.0.0"]
                 [org.iplantc/kameleon "5.0.0"]
                 [org.iplantc/heuristomancer "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]]
  :plugins [[lein-ring "0.9.2" :exclusions [org.clojure/clojure]]
            [swank-clojure "1.4.2" :exclusions [org.clojure/clojure]]]
  :profiles {:dev     {:resource-paths ["conf/test"]}
             :uberjar {:aot :all}}
  :main ^:skip-aot donkey.core
  :ring {:handler donkey.core/app
         :init donkey.core/lein-ring-init
         :port 31325
         :auto-reload? false}
  :uberjar-exclusions [#".*[.]SF" #"LICENSE" #"NOTICE"]
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]
                 ["biojava"
                  {:url "http://www.biojava.org/download/maven"}]
                 ["nexml"
                  {:url "http://nexml-dev.nescent.org/.m2/repository"
                   :checksum :ignore
                   :update :never}]]
  :jvm-opts ["-Dlogback.configurationFile=/etc/iplant/de/logging/donkey-logging.xml"])
