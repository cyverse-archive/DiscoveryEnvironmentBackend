(defproject fishy "0.1.0-SNAPSHOT"
  :description "A REST front-end for Grouper."
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [clj-http "1.1.2"]
                 [com.cemerick/url "0.1.1"]
                 [metosin/compojure-api "0.22.1"]
                 [org.iplantc/clojure-commons "5.0.0"]
                 [org.iplantc/service-logging "5.0.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]]
  :plugins [[lein-ring "0.9.6"]]
  :profiles {:dev {:resource-paths ["conf/test"]}}
  :aot [fishy.core]
  :main fishy.core
  :ring {:handler fishy.routes/app
         :init    fishy.core/load-config-from-file
         :port    31310})
