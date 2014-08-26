(defproject org.iplantc/infosquito "3.2.4"
  :description "An ICAT database crawler used to index the contents of iRODS."
  :url "http://www.iplantcollaborative.org"
  :license {:name "BSD"
            :url "http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :aot [infosquito.core]
  :main infosquito.core
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [postgresql "9.1-901-1.jdbc4"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.clojure/tools.logging "0.3.0"]
                 [cheshire "5.3.1"]
                 [clojurewerkz/elastisch "2.0.0"]
                 [com.novemberain/langohr "2.11.0"]
                 [slingshot "0.10.3"]
                 [me.raynes/fs "1.4.6"]
                 [org.iplantc/clojure-commons "3.2.4"]
                 [org.iplantc/common-cli "3.2.4"]]
  :profiles {:dev {:resource-paths ["dev-resources"]}}
  :plugins [[org.iplantc/lein-iplant-rpm "3.2.4"]]
  :iplant-rpm {:summary      "infosquito"
               :dependencies ["iplant-service-config >= 0.1.0-5" "java-1.7.0-openjdk"]
               :config-files ["log4j.properties"]
               :config-path  "conf"}
  :repositories [["sonatype-nexus-snapshots"
                  {:url "https://oss.sonatype.org/content/repositories/snapshots"}]]
  :deploy-repositories [["sonatype-nexus-staging"
                         {:url "https://oss.sonatype.org/service/local/staging/deploy/maven2/"}]
                        ["renci.repository"
                         {:url "http://ci-dev.renci.org/nexus/content/repositories/releases/"}]])
