(defproject chinstrap "0.2.1-SNAPSHOT"
  :description "Diagnostic and status front-end for backend data"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.iplantc/clojure-commons "1.4.1-SNAPSHOT"]
                 [org.iplantc/kameleon "0.1.1-SNAPSHOT"]
                 [clj-time "0.4.5"]
                 [com.novemberain/monger "1.4.2"]
                 [korma/korma "0.3.0-RC2"]
                 [log4j/log4j "1.2.17"]
                 [noir "1.3.0-beta10"]]
  :plugins [[org.iplantc/lein-iplant-rpm "1.4.1-SNAPSHOT"]]
  :profiles {:dev {:resource-paths ["resources/conf/test"]}}
  :aot [chinstrap.server]
  :main chinstrap.server
  :iplant-rpm {:summary "iPlant Chinstrap"
               :provides "chinstrap"
               :dependencies ["iplant-service-config >= 0.1.0-5"]
               :config-files ["log4j.properties"]
               :config-path "resources/conf/main"}
  :repositories {"iplantCollaborative"
                 "http://projects.iplantcollaborative.org/archiva/repository/internal/"})
