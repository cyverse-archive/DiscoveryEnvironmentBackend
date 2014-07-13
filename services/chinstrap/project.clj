(defproject chinstrap "3.1.9"
  :description "Diagnostic and status front-end for backend data"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.iplantc/clojure-commons "3.1.9"]
                 [org.iplantc/kameleon "3.1.9"]
                 [clj-time "0.7.0"]
                 [com.novemberain/monger "2.0.0"]
                 [korma/korma "0.3.0-RC5"]
                 [log4j/log4j "1.2.17"]
                 [lib-noir "0.8.3"]
                 [compojure "1.1.8"]
                 [ring "1.3.0"]]
  :plugins [[org.iplantc/lein-iplant-rpm "3.1.9"]]
  :profiles {:dev {:resource-paths ["resources/conf/test"]}}
  :aot [chinstrap.server]
  :main chinstrap.server
  :iplant-rpm {:summary "iPlant Chinstrap"
               :provides "chinstrap"
               :dependencies ["iplant-service-config >= 0.1.0-5"]
               :config-files ["log4j.properties"]
               :config-path "resources/conf/main"}
  :repositories {"iplantCollaborative"
                 "http://katic.iplantcollaborative.org/archiva/repository/internal/"})
